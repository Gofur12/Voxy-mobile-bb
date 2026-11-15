package me.cortex.voxy.client;

import me.cortex.voxy.client.core.gl.Capabilities;
import me.cortex.voxy.client.core.gl.backend.GlBackendRuntime;
import me.cortex.voxy.client.core.gl.backend.desktop.DesktopGlBackendProvider;
import me.cortex.voxy.client.core.rendering.util.SharedIndexBuffer;
import me.cortex.voxy.client.core.rendering.util.UploadStream;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.commonImpl.VoxyCommon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Function;

public class VoxyClient implements ClientModInitializer {
    private static final HashSet<String> FREX = new HashSet<>();
    private static FileLock EXCLUSIVE_LOCK;
    public static void initVoxyClient() {
        // Called after RenderSystem.initRenderer returns; the game context is current here.
        if (!GlBackendRuntime.initialize(new DesktopGlBackendProvider())) {
            Logger.error("Voxy is unsupported: no compatible GPU backend is available.");
            return;
        }

        Capabilities.init();//Capture capabilities for this active graphics context

        if (Capabilities.require().hasBrokenDepthSampler) {
            Logger.error("AMD broken depth sampler detected, voxy does not work correctly and has been disabled, this will hopefully be fixed in the future");
        }

        boolean systemSupported = Capabilities.require().compute && Capabilities.require().indirectParameters && !Capabilities.require().hasBrokenDepthSampler;
        if (!systemSupported) {
             Logger.error("Voxy is unsupported on your system.");
        }

        if (systemSupported && System.getProperty("voxy.exclusiveLock", "false").equalsIgnoreCase("true")) {
            //Try acquire the lock file
            var vf = Minecraft.getInstance().gameDirectory.toPath().resolve(".voxy");
            if (!vf.toFile().isDirectory()) {
                vf.toFile().mkdir();
            }
            try {
                FileOutputStream fis = new FileOutputStream(vf.resolve("voxy.lock").toFile());
                EXCLUSIVE_LOCK = fis.getChannel().lock(0, Long.MAX_VALUE, false);
            } catch (NonWritableChannelException | IOException e) {
                //If some error write to log and unsupport
                Logger.error("Failed to acquire exclusive voxy lock file, mod will be disabled");
                systemSupported = false;
            }

        }

        if (systemSupported) {

            UploadStream.initialize();
            SharedIndexBuffer.initialize();

            VoxyCommon.setInstanceFactory(VoxyClientInstance::new);

            if (!Capabilities.require().subgroup) {
                Logger.warn("GPU does not support subgroup operations, expect some performance degradation");
            }

        }
    }

    /**
     * Releases Voxy's world instance and backend while Minecraft still owns
     * the graphics context. Called from the client shutdown hook.
     */
    public static void shutdownGraphicsResources() {
        RuntimeException shutdownFailure = null;
        try {
            VoxyCommon.shutdownInstance();
        } catch (RuntimeException failure) {
            shutdownFailure = failure;
            Logger.error("Failed to shut down Voxy instance before graphics teardown", failure);
        }

        try {
            SharedIndexBuffer.shutdown();
        } catch (RuntimeException failure) {
            if (shutdownFailure != null) shutdownFailure.addSuppressed(failure);
            else shutdownFailure = failure;
            Logger.error("Failed to release shared index buffers", failure);
        }

        try {
            UploadStream.shutdown();
        } catch (RuntimeException failure) {
            if (shutdownFailure != null) shutdownFailure.addSuppressed(failure);
            else shutdownFailure = failure;
            Logger.error("Failed to shut down upload stream", failure);
        }

        Capabilities.clear();

        if (GlBackendRuntime.isInitialized()) {
            try {
                GlBackendRuntime.shutdown();
            } catch (RuntimeException failure) {
                if (shutdownFailure != null) shutdownFailure.addSuppressed(failure);
                else shutdownFailure = failure;
                Logger.error("Failed to shut down Voxy graphics backend", failure);
            }
        }

        if (shutdownFailure != null) {
            Logger.error("Voxy graphics shutdown completed with errors");
        }
    }

    @Override
    public void onInitializeClient() {
        DebugEntries.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            if (VoxyCommon.isAvailable()) {
                dispatcher.register(VoxyCommands.register());
            }
        });

        FabricLoader.getInstance()
                .getEntrypoints("frex_flawless_frames", Consumer.class)
                .forEach(api -> ((Consumer<Function<String,Consumer<Boolean>>>)api).accept(name->active->{if (active) {
                    FREX.add(name);
                } else {
                    FREX.remove(name);
                }}));
    }

    public static boolean isFrexActive() {
        return !FREX.isEmpty();
    }

    public static int getOcclusionDebugState() {
        return 0;
    }

    public static boolean disableSodiumChunkRender() {
        return false;// getOcclusionDebugState() != 0;
    }
}