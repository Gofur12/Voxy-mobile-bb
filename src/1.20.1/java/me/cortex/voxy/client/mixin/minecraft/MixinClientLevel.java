package me.cortex.voxy.client.mixin.minecraft;

import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.common.world.service.VoxelIngestService;
import me.cortex.voxy.commonImpl.VoxyCommon;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 1.20.1 variant. The constructor hook is dropped (the 1.20.1 constructor
 * signature differs); the bottom section is read from Level#getMinSection instead.
 */
@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {
    @Inject(method = "setBlocksDirty", at = @At("TAIL"))
    private void voxy$injectIngestOnStateChange(BlockPos pos, BlockState old, BlockState updated, CallbackInfo ci) {
        if (old == updated) return;
        if (!updated.isAir()) return;
        if (VoxyCommon.getInstance() == null) return;
        if (!VoxyConfig.CONFIG.ingestEnabled) return;

        var self = (Level) (Object) this;
        var wi = WorldIdentifier.of(self);
        if (wi == null) {
            return;
        }

        int x = pos.getX() & 15;
        int y = pos.getY() & 15;
        int z = pos.getZ() & 15;
        if (x == 0 || x == 15 || y == 0 || y == 15 || z == 0 || z == 15) {
            var csp = SectionPos.of(pos);
            var chunk = self.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
            if (chunk != null) {
                var section = chunk.getSection(csp.getY() - self.getMinSection());
                var lp = self.getLightEngine();

                var blp = lp.getLayerListener(LightLayer.BLOCK).getDataLayerData(csp);
                var slp = lp.getLayerListener(LightLayer.SKY).getDataLayerData(csp);

                VoxelIngestService.rawIngest(wi, section, csp.getX(), csp.getY(), csp.getZ(),
                        blp == null ? null : blp.copy(), slp == null ? null : slp.copy());
            }
        }
    }
}
