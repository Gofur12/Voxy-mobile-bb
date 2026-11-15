package me.cortex.voxy.client.mixin.sodium;

import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IVoxyRenderSystemHolder;
import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.client.core.util.IrisUtil;
import me.cortex.voxy.common.world.service.VoxelIngestService;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public class MixinRenderSectionManager {
    @Unique
    private static final boolean BOBBY_INSTALLED = FabricLoader.getInstance().isModLoaded("bobby");

    @Shadow @Final private ClientLevel world;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void voxy$resetChunkTracker(ClientLevel world, int renderDistance, CommandList commandList, CallbackInfo ci) {
        this.bottomSectionY = this.world.getMinBuildHeight() >> 4;
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void voxy$resetVisibleSectionStream(Camera camera, Viewport viewport, int frame, boolean spectator, CallbackInfo ci) {
        VoxyRenderSystem renderer = IVoxyRenderSystemHolder.getNullable();
        if (renderer != null && !IrisUtil.irisShadowActive() && renderer.visbleSectionStream != null) {
            renderer.visbleSectionStream.reset();
        }
    }

    @Inject(method = "onChunkRemoved", at = @At("HEAD"))
    private void voxy$ingestBeforeChunkRemoval(int x, int z, CallbackInfo ci) {
        if (!VoxyConfig.CONFIG.ingestEnabled || BOBBY_INSTALLED || this.world == null) return;
        LevelChunk chunk = this.world.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
        if (chunk != null) {
            VoxelIngestService.tryAutoIngestChunk(chunk);
        }
    }

    @Inject(method = "onChunkAdded", at = @At("HEAD"))
    private void voxy$ingestOnChunkAdded(int x, int z, CallbackInfo ci) {
        if (!VoxyConfig.CONFIG.ingestEnabled || this.world == null) return;
        LevelChunk chunk = this.world.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
        if (chunk != null) {
            VoxelIngestService.tryAutoIngestChunk(chunk);
        }
    }

    @Unique private long cachedChunkPos = Long.MIN_VALUE;
    @Unique private int cachedChunkStatus;
    @Unique private int bottomSectionY;

    @Redirect(
            method = "updateSectionInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/RenderSection;setInfo(Lme/jellysquid/mods/sodium/client/render/chunk/data/BuiltSectionInfo;)V"
            ),
            remap = false
    )
    private void voxy$ingestNewlyBuiltSection(RenderSection section, BuiltSectionInfo info) {
        boolean wasBuilt = section.isBuilt();
        section.setInfo(info);

        VoxyRenderSystem renderer = IVoxyRenderSystemHolder.getNullable();
        if (wasBuilt || !section.isBuilt() || renderer == null || !VoxyConfig.CONFIG.ingestEnabled) {
            return;
        }

        int x = section.getChunkX();
        int y = section.getChunkY();
        int z = section.getChunkZ();

        var tracker = ((AccessorChunkTracker) ChunkTrackerHolder.get(this.world)).getChunkStatus();
        long key = ChunkPos.asLong(x, z);
        if (key != this.cachedChunkPos) {
            this.cachedChunkPos = key;
            this.cachedChunkStatus = tracker.getOrDefault(key, 0);
        }

        // Status 3 means this chunk has the full surrounding neighborhood needed for stable ingestion.
        if (this.cachedChunkStatus != 3) return;

        LevelChunk chunk = this.world.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
        if (chunk == null) return;

        var chunkSection = chunk.getSection(y - this.bottomSectionY);
        var lightEngine = this.world.getLightEngine();
        var sectionPos = SectionPos.of(x, y, z);
        var blockLight = lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(sectionPos);
        var skyLight = lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(sectionPos);

        VoxelIngestService.rawIngest(
                renderer.getEngine(),
                chunkSection,
                x, y, z,
                blockLight == null ? null : blockLight.copy(),
                skyLight == null ? null : skyLight.copy()
        );
    }
}
