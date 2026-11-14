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
    private final AllocationArena allocationArena = new AllocationArena();
    private final ArrayList<DownloadFragment> frameFragments = new ArrayList<>();
    private final Deque<DownloadFrame> frames = new ArrayDeque<>();

    public RawDownloadStream(int size) {
        if (size <= 0) throw new IllegalArgumentException("Raw download stream size must be positive");
        GlBackend backend = GlBackendRuntime.requireBackend();
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

    public int download(int size, IDownloadCompletedCallback callback) {
        if (size <= 0) throw new IllegalArgumentException("Download size must be positive");
        if (callback == null) throw new NullPointerException("callback");
        int allocation = (int) this.allocationArena.alloc(size);
        if (allocation == AllocationArena.SIZE_LIMIT) {
            Logger.warn("Raw download stream full; waiting for submitted download frames");
            this.submit();
            GlBackendRuntime.requireBackend().finish();
            this.tick();
            allocation = (int) this.allocationArena.alloc(size);
            if (allocation == AllocationArena.SIZE_LIMIT) {
                throw new IllegalStateException("Unable to free enough memory for raw download stream");
            }
        }
        this.frameFragments.add(new DownloadFragment(allocation, size, callback));
        return allocation;
    }

    // Submits the current group and fences writes issued by the caller.
    public void submit() {
        if (!this.frameFragments.isEmpty()) {
            DownloadFragment[] fragments = this.frameFragments.toArray(new DownloadFragment[0]);
            this.frameFragments.clear();
            this.frames.add(new DownloadFrame(new GlFence(), fragments));
        }
    }

    public void tick() {
        this.submit();
        GlBackend backend = GlBackendRuntime.requireBackend();
        while (!this.frames.isEmpty()) {
            DownloadFrame frame = this.frames.peekFirst();
            if (!frame.fence.signaled()) break;
            this.frames.removeFirst();
            try {
                for (DownloadFragment fragment : frame.fragments) {
                    if (this.persistentMapping) {
                        long address = this.persistentBuffer.addr() + fragment.allocation;
                        fragment.callback.accept(address);
                    } else {
                        this.deliverCpu(fragment, backend);
                    }
                }
            } finally {
                for (DownloadFragment fragment : frame.fragments) {
                    this.allocationArena.free(fragment.allocation);
                }
                frame.fence.free();
            }
        }
    }

    private void deliverCpu(DownloadFragment fragment, GlBackend backend) {
        long destination = MemoryUtil.nmemAlloc(fragment.size);
        if (destination == 0L) throw new OutOfMemoryError("Unable to allocate raw download CPU staging memory");
        long mapped = 0L;
        boolean opened = false;
        boolean copied = false;
        try {
            mapped = backend.mapBufferRange(this.cpuReadbackBuffer.id, fragment.allocation,
                    fragment.size, GL_MAP_READ_BIT);
            if (mapped == 0L) throw new IllegalStateException("Backend returned a null readback mapping");
            opened = true;
            MemoryUtil.memCopy(mapped, destination, fragment.size);
            copied = true;
        } finally {
            boolean valid = true;
            if (opened) valid = backend.unmapBuffer(this.cpuReadbackBuffer.id);
            if (!copied || !valid) MemoryUtil.nmemFree(destination);
            if (!valid) throw new IllegalStateException("Raw download buffer became invalid during readback");
        }
        try {
            fragment.callback.accept(destination);
        } finally {
            MemoryUtil.nmemFree(destination);
        }
    }

    public int getBufferId() {
        return this.persistentMapping ? this.persistentBuffer.id : this.cpuReadbackBuffer.id;
    }

    public void free() {
        this.submit();
        GlBackend backend = GlBackendRuntime.requireBackend();
        backend.finish();
        this.tick();
        if (!this.frames.isEmpty()) throw new IllegalStateException("Raw download frames remain after GPU finish");
        if (this.persistentMapping) {
            this.persistentBuffer.free();
        } else {
            this.cpuReadbackBuffer.free();
        }
    }
}
