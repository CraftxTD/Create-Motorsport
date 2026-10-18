package com.createmotorsport.block;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;
import java.util.Set;

public final class ConfiguredDrops {

    private ConfiguredDrops() {
    }

    public static List<ItemStack> attach(List<ItemStack> drops, LootParams.Builder builder, Block block,
                                         Set<String> keys) {
        if (!(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BlockEntity be)) {
            return drops;
        }
        CompoundTag saved = be.saveCustomOnly(builder.getLevel().registryAccess());
        CompoundTag kept = new CompoundTag();
        for (String key : keys) {
            if (saved.contains(key)) {
                kept.put(key, saved.get(key));
            }
        }
        BlockEntity.addEntityType(kept, be.getType());
        for (ItemStack stack : drops) {
            if (stack.is(block.asItem())) {
                stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(kept));
            }
        }
        return drops;
    }
}
