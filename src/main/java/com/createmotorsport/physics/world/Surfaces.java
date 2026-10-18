package com.createmotorsport.physics.world;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.physics.spec.SurfaceSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

public final class Surfaces {

    private static TagKey<Block> tag(String name) {
        return TagKey.create(BlockTags.DIRT.registry(),
                ResourceLocation.fromNamespaceAndPath(CreateMotorsport.MODID, "surface/" + name));
    }

    public static final TagKey<Block> DIRT = tag("dirt");
    public static final TagKey<Block> GRAVEL = tag("gravel");
    public static final TagKey<Block> SAND = tag("sand");

    private Surfaces() {
    }

    public static void checkTags(TagsUpdatedEvent event) {
        for (TagKey<Block> tag : java.util.List.of(DIRT, GRAVEL, SAND)) {
            if (BuiltInRegistries.BLOCK.getTag(tag).map(t -> t.size() == 0).orElse(true)) {
                CreateMotorsport.LOGGER.warn(
                        "Surface tag {} is empty or missing, mark it \"required\": false.", tag.location());
            }
        }
    }

    public static SurfaceSpec of(Level level, BlockPos pos, BlockState state) {
        boolean wet = level != null && level.isRaining() && level.canSeeSky(pos.above());
        if (state.is(DIRT)) {
            return wet ? SurfaceSpec.WET_DIRT : SurfaceSpec.DRY_DIRT;
        }
        if (state.is(GRAVEL)) {
            return wet ? SurfaceSpec.WET_GRAVEL : SurfaceSpec.DRY_GRAVEL;
        }
        if (state.is(SAND)) {
            return wet ? SurfaceSpec.WET_SAND : SurfaceSpec.DRY_SAND;
        }
        return wet ? SurfaceSpec.WET_DEFAULT : SurfaceSpec.DRY_DEFAULT;
    }
}
