package me.cortex.voxy.client.mixin.sodium;

import me.cortex.voxy.client.core.IVoxyRenderSystemHolder;
import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class MixinDefaultChunkRenderer extends ShaderChunkRenderer {
    public MixinDefaultChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);
    }

    /**
     * Sodium 0.5.13 has no RenderPearl targets or modern OIT parameters.
     * Voxy draws after the vanilla cutout pass while the main render target is active.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void voxy$injectRender(
            ChunkRenderMatrices matrices,
            me.jellysquid.mods.sodium.client.gl.device.CommandList commandList,
            ChunkRenderListIterable renderLists,
            TerrainRenderPass renderPass,
            CameraTransform camera,
            CallbackInfo ci
    ) {
        if (renderPass != DefaultTerrainRenderPasses.CUTOUT) return;

        VoxyRenderSystem renderer = IVoxyRenderSystemHolder.getNullable();
        if (renderer == null) return;

        var target = Minecraft.getInstance().getMainRenderTarget();
        renderer.renderOpaque(
                renderer.setupViewport(
                        matrices.projection(),
                        matrices.modelView(),
                        null,
                        target.width,
                        target.height,
                        camera.x,
                        camera.y,
                        camera.z
                ),
                target.getDepthTextureId(),
                target.getColorTextureId()
        );
    }
}
