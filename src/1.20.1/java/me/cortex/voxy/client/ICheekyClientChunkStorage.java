package me.cortex.voxy.client;

import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/**
 * Implemented onto ClientChunkCache$Storage (1.20.1) by a mixin so chunks can be
 * read without the range check. Lives outside the mixin package on purpose.
 */
public interface ICheekyClientChunkStorage {
    @Nullable
    LevelChunk voxy$getChunkAt(int x, int z);
}
