package me.cortex.voxy.common.voxelization;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import me.cortex.voxy.common.world.other.Mapper;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;

import java.util.WeakHashMap;

/**
 * Minecraft 1.20.1 implementation of section conversion.
 *
 * The 1.20.1 PalettedContainer keeps its packed data and internal Data type
 * private. Read through the stable public get(x, y, z) API instead of linking
 * the modern implementation to those private internals.
 */
public class WorldConversionFactory {
    private static final class Cache {
        private final int[] biomeCache = new int[4 * 4 * 4];
        private final WeakHashMap<Mapper, Reference2IntOpenHashMap<BlockState>> localMapping = new WeakHashMap<>();

        private Reference2IntOpenHashMap<BlockState> getLocalMapping(Mapper mapper) {
            return this.localMapping.computeIfAbsent(mapper, ignored -> new Reference2IntOpenHashMap<>());
        }
    }

    private static final ThreadLocal<Cache> THREAD_LOCAL = ThreadLocal.withInitial(Cache::new);

    public static VoxelizedSection convert(VoxelizedSection section,
                                           Mapper stateMapper,
                                           PalettedContainer<BlockState> blockContainer,
                                           PalettedContainerRO<Holder<Biome>> biomeContainer,
                                           ILightingSupplier lightSupplier) {
        return convert(section, stateMapper, blockContainer, biomeContainer, lightSupplier, false, 0L);
    }

    public static VoxelizedSection convert(VoxelizedSection section,
                                           Mapper stateMapper,
                                           PalettedContainer<BlockState> blockContainer,
                                           PalettedContainerRO<Holder<Biome>> biomeContainer,
                                           ILightingSupplier lightSupplier,
                                           boolean shouldZoom,
                                           long zoomSeed) {
        Cache cache = THREAD_LOCAL.get();
        Reference2IntOpenHashMap<BlockState> blockCache = cache.getLocalMapping(stateMapper);
        int[] biomes = cache.biomeCache;

        // Biome container coordinates are 4x4x4, in y-z-x iteration order.
        int biomeIndex = 0;
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 0; x < 4; x++) {
                    biomes[biomeIndex++] = stateMapper.getIdForBiome(biomeContainer.get(x, y, z));
                }
            }
        }

        int nonAirCount = 0;
        long[] output = section.section;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = blockContainer.get(x, y, z);
                    int blockId = blockCache.getOrDefault(state, -1);
                    if (blockId == -1) {
                        blockId = stateMapper.getIdForBlockState(state);
                        blockCache.put(state, blockId);
                    }

                    int biomeId = biomes[(y >> 2) * 16 + (z >> 2) * 4 + (x >> 2)];
                    byte light = lightSupplier.supply(x, y, z);
                    int index = (y << 8) | (z << 4) | x;
                    if (blockId != 0) {
                        nonAirCount++;
                        output[index] = Mapper.composeMappingId(light, blockId, biomeId);
                    } else {
                        output[index] = Mapper.airWithLight(light);
                    }
                }
            }
        }

        section.lvl0NonAirCount = nonAirCount;
        return section;
    }

    @Deprecated(forRemoval = true)
    public static void mipSection(VoxelizedSection section, Mapper mapper) {
        WorldVoxilizedSectionMipper.mipSection(section, mapper);
    }
}
