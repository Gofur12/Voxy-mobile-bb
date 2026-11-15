package me.cortex.voxy.client.mixin.minecraft;

import me.cortex.voxy.client.ICheekyClientChunkStorage;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 1.20.1: ClientChunkCache$Storage is a package-private final inner class whose
 * getChunk/getIndex are not public. Expose them through our own interface instead
 * of calling them directly.
 */
@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public abstract class MixinClientChunkCacheStorage implements ICheekyClientChunkStorage {
    @Shadow
    protected abstract LevelChunk getChunk(int index);

    @Shadow
    abstract int getIndex(int x, int z);

    @Override
    public LevelChunk voxy$getChunkAt(int x, int z) {
        return this.getChunk(this.getIndex(x, z));
    }
}
