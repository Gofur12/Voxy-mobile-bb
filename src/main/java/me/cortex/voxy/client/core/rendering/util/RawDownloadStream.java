package me.cortex.voxy.client.core.rendering.util;

import me.cortex.voxy.client.core.gl.GlBuffer;
import me.cortex.voxy.client.core.gl.GlFence;
import me.cortex.voxy.client.core.gl.GlPersistentMappedBuffer;
import me.cortex.voxy.client.core.gl.backend.GlBackend;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.util.AllocationArena;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import static org.lwjgl.opengl.GL30C.GL_MAP_READ_BIT;
import static org.lwjgl.opengl.GL44C.GL_MAP_COHERENT_BIT;

// Raw download stream. The returned allocation is a GPU buffer offset.
public class RawDownloadStream {
    // The pointer is valid only for the duration of the callback.
    public interface IDownloadCompletedCallback { void accept(long ptr); }
    private record DownloadFragment(int allocation, int size, IDownloadCompletedCallback callback) {}
    private record DownloadFrame(GlFence fence, DownloadFragment[] fragments) {}

    private final GlPersistentMappedBuffer persistentBuffer;
    private final GlBuffer cpuReadbackBuffer;
    private final boolean persistentMapping;
    private final long contextGeneration;
    private boolean closed;
    private final AllocationArena allocationArena = new AllocationArena();
    private final ArrayList<DownloadFragment> frameFragments = new ArrayList<>();
    private final Deque<DownloadFrame> frames = new ArrayDeque<>();

    public RawDownloadStream(int size) {
        if (size <= 0) throw new IllegalArgumentException("Raw download stream size must be positive");
        GlBackend backend = GlBackendRuntime.requireBackend();
        this.contextGeneration = GlBackendRuntime.getContextGeneration();
        var capabilities = backend.capabilities();
        this.persistentMapping = capabilities.bufferStorage && capabilities.persistentMapping;
        if (this.persistentMapping) {
            this.persistentBuffer = new GlPersistentMappedBuffer(size, GL_MAP_READ_BIT | GL_MAP_COHERENT_BIT).name("RawDownloadStream");
            this.cpuReadbackBuffer = null;
        } else {
            this.persistentBuffer = null;
            this.cpuReadbackBuffer = new GlBuffer(size, 0, false).name("RawDownloadStream-CPU");
        }
        this.allocationArena.setLimit(size);
    }

    private void requireCurrentContext() {
        if (this.closed) throw new IllegalStateException("RawDownloadStream has been freed");
        GlBackendRuntime.requireBackend(this.contextGeneration, "RawDownloadStream");
    }

    public int download(int size, IDownloadCompletedCallback callback) {
        this.requireCurrentContext();
        if (size <= 0) throw new IllegalArgumentException("Download size must be positive");
        if (callback == null) throw new NullPointerException("callback");
        int allocation = (int) this.allocationArena.alloc(size);
        if (allocation == AllocationArena.SIZE_LIMIT) {
            Logger.warn("Raw download stream full; waiting for submitted download frames");
            this.submit();
            GlBackendRuntime.requireBackend(this.contextGeneration, "RawDownloadStream").finish();
            this.tick();
            allocation = (int) this.allocationArena.alloc(size);
            if (allocation == AllocationArena.SIZE_LIMIT) {
                throw new IllegalStateException("Unable to free enough memory for raw download stream");
            }
        }
        try {
            this.frameFragments.add(new DownloadFragment(allocation, size, callback));
        } catch (RuntimeException | Error trackingFailure) {
            try {
                this.allocationArena.free(allocation);
            } catch (RuntimeException | Error cleanupFailure) {
                if (trackingFailure != cleanupFailure) trackingFailure.addSuppressed(cleanupFailure);
            }
            throw trackingFailure;
        }
        return allocation;
    }

    // Submits the current group and fences writes issued by the caller.
    public void submit() {
        this.requireCurrentContext();
        if (!this.frameFragments.isEmpty()) {
            // Keep the source fragments owned by this stream until both the
            // fence and its frame have been created successfully.
            DownloadFragment[] fragments = this.frameFragments.toArray(new DownloadFragment[0]);
            GlFence fence = new GlFence();
            try {
                this.frames.add(new DownloadFrame(fence, fragments));
            } catch (RuntimeException | Error enqueueFailure) {
                try {
                    fence.free();
                } catch (RuntimeException | Error fenceFailure) {
                    if (enqueueFailure != fenceFailure) enqueueFailure.addSuppressed(fenceFailure);
                }
                throw enqueueFailure;
            }
            this.frameFragments.clear();
        }
    }

    public void tick() {
        this.requireCurrentContext();
        this.submit();
        GlBackend backend = GlBackendRuntime.requireBackend(this.contextGeneration, "RawDownloadStream");
        Throwable failure = null;
        while (!this.frames.isEmpty()) {
            DownloadFrame frame = this.frames.peekFirst();
            if (!frame.fence.signaled()) break;
            this.frames.removeFirst();
            for (DownloadFragment fragment : frame.fragments) {
                try {
                    if (this.persistentMapping) {
                        long address = this.persistentBuffer.addr() + fragment.allocation;
                        fragment.callback.accept(address);
                    } else {
                        this.deliverCpu(fragment, backend);
                    }
                } catch (RuntimeException | Error callbackFailure) {
                    failure = appendFailure(failure, callbackFailure);
                }
            }
            for (DownloadFragment fragment : frame.fragments) {
                try {
                    this.allocationArena.free(fragment.allocation);
                } catch (RuntimeException | Error cleanupFailure) {
                    failure = appendFailure(failure, cleanupFailure);
                }
            }
            try {
                frame.fence.free();
            } catch (RuntimeException | Error cleanupFailure) {
                failure = appendFailure(failure, cleanupFailure);
            }
        }
        rethrowFailure(failure);
    }

    private void deliverCpu(DownloadFragment fragment, GlBackend backend) {
        long destination = MemoryUtil.nmemAlloc(fragment.size);
        if (destination == 0L) throw new OutOfMemoryError("Unable to allocate raw download CPU staging memory");
        long mapped = 0L;
        Throwable failure = null;
        boolean valid = true;
        try {
            mapped = backend.mapBufferRange(this.cpuReadbackBuffer.id, fragment.allocation,
                    fragment.size, GL_MAP_READ_BIT);
            if (mapped == 0L) throw new IllegalStateException("Backend returned a null readback mapping");
            MemoryUtil.memCopy(mapped, destination, fragment.size);
        } catch (RuntimeException | Error readbackFailure) {
            failure = readbackFailure;
        } finally {
            if (mapped != 0L) {
                try {
                    valid = backend.unmapBuffer(this.cpuReadbackBuffer.id);
                    if (!valid) {
                        failure = appendFailure(failure,
                                new IllegalStateException("Raw download buffer became invalid during readback"));
                    }
                } catch (RuntimeException | Error unmapFailure) {
                    failure = appendFailure(failure, unmapFailure);
                }
            }
        }
        if (failure != null) {
            MemoryUtil.nmemFree(destination);
            rethrowFailure(failure);
        }
        try {
            fragment.callback.accept(destination);
        } finally {
            MemoryUtil.nmemFree(destination);
        }
    }

    private static Throwable appendFailure(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }

    private static void rethrowFailure(Throwable failure) {
        if (failure == null) return;
        if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
        if (failure instanceof Error errorFailure) throw errorFailure;
        throw new IllegalStateException(failure);
    }

    public int getBufferId() {
        this.requireCurrentContext();
        return this.persistentMapping ? this.persistentBuffer.id : this.cpuReadbackBuffer.id;
    }

    public void free() {
        if (this.closed) return;
        Throwable failure = null;
        boolean contextCurrent = GlBackendRuntime.isContextGenerationCurrent(this.contextGeneration);
        boolean gpuQuiesced = false;

        if (contextCurrent) {
            try {
                this.submit();
            } catch (RuntimeException | Error submitFailure) {
                failure = appendFailure(failure, submitFailure);
            }
            try {
                GlBackendRuntime.requireBackend(this.contextGeneration, "RawDownloadStream").finish();
                gpuQuiesced = true;
            } catch (RuntimeException | Error finishFailure) {
                failure = appendFailure(failure, finishFailure);
            }
            if (gpuQuiesced) {
                try {
                    this.tick();
                } catch (RuntimeException | Error callbackFailure) {
                    failure = appendFailure(failure, callbackFailure);
                }
            }
        }

        this.closed = true;
        if (gpuQuiesced) {
            // A successful finish also makes unsubmitted fragment allocations safe to retire.
            for (DownloadFragment fragment : this.frameFragments) {
                try {
                    this.allocationArena.free(fragment.allocation);
                } catch (RuntimeException | Error cleanupFailure) {
                    failure = appendFailure(failure, cleanupFailure);
                }
            }
        }
        // Drop callback references even when the context is lost or GPU completion cannot be established.
        this.frameFragments.clear();
        if (gpuQuiesced) {
            // A successful finish makes all remaining arena allocations and
            // fences safe to retire, including frames left by a failed tick.
            while (!this.frames.isEmpty()) {
                DownloadFrame frame = this.frames.removeFirst();
                for (DownloadFragment fragment : frame.fragments) {
                    try {
                        this.allocationArena.free(fragment.allocation);
                    } catch (RuntimeException | Error cleanupFailure) {
                        failure = appendFailure(failure, cleanupFailure);
                    }
                }
                try {
                    frame.fence.free();
                } catch (RuntimeException | Error fenceFailure) {
                    failure = appendFailure(failure, fenceFailure);
                }
            }
            if (this.persistentMapping) {
                try {
                    this.persistentBuffer.free();
                } catch (RuntimeException | Error bufferFailure) {
                    failure = appendFailure(failure, bufferFailure);
                }
            } else {
                try {
                    this.cpuReadbackBuffer.free();
                } catch (RuntimeException | Error bufferFailure) {
                    failure = appendFailure(failure, bufferFailure);
                }
            }
        } else {
            // Do not issue GL deletion after context loss or failed glFinish:
            // GPU use of the buffer/fences cannot be proven complete.
            this.frames.clear();
        }
        rethrowFailure(failure);
    }
}
