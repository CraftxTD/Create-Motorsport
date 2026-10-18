package com.createmotorsport.item;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.physics.spec.TireSpec;
import com.createmotorsport.registry.SpecCodecs;
import com.createmotorsport.registry.SpecOverrides;
import com.createmotorsport.registry.SpecRegistries;
import com.createmotorsport.registry.TireEntry;
import com.createmotorsport.registry.TireFit;
import com.createmotorsport.tune.Tunable;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class HeldTire implements Tunable {

    private static final ResourceLocation AXLE_DEFAULT =
            ResourceLocation.fromNamespaceAndPath(CreateMotorsport.MODID, "axle_default");

    private final ItemStack stack;
    @Nullable
    private final RegistryAccess access;

    public HeldTire(ItemStack stack, @Nullable RegistryAccess access) {
        this.stack = stack;
        this.access = access;
    }

    public static boolean tunable(ItemStack stack) {
        return stack.getItem() instanceof TireItem && stack.has(CreateMotorsport.TIRE_DESIGN_LOAD);
    }

    private TireFit pack() {
        TireEntry entry = SpecRegistries.resolve(access, SpecRegistries.TIRE, stack.get(CreateMotorsport.TIRE), null);
        return entry != null ? TireFit.of(entry) : TireFit.of(new ItemStack(stack.getItem()));
    }

    @Override
    public List<Section<?>> tuneSections() {
        TireFit pack = pack();
        ResourceLocation tire = stack.get(CreateMotorsport.TIRE);
        ResourceLocation compound = stack.get(CreateMotorsport.TIRE_COMPOUND);
        return List.of(
                new Section<>("tire", "Tyre", tire != null ? tire : BuiltInRegistries.ITEM.getKey(stack.getItem()),
                        SpecCodecs.TIRE_FIT, pack, SpecOverrides.diff(SpecCodecs.TIRE_FIT, pack, TireFit.of(stack)),
                        tag -> SpecOverrides.apply(SpecCodecs.TIRE_FIT, pack, tag).fit(stack), null),
                new Section<>("compound", "Compound", compound != null ? compound : AXLE_DEFAULT,
                        SpecCodecs.TIRE_SPEC, SpecRegistries.fittedCompound(access, stack, true),
                        stack.getOrDefault(CreateMotorsport.TIRE_COMPOUND_OVERRIDES, new CompoundTag()),
                        this::storeCompoundOverrides, this::cycleCompound));
    }

    private void storeCompoundOverrides(CompoundTag overrides) {
        if (!stack.has(CreateMotorsport.TIRE_COMPOUND)) {
            stack.set(CreateMotorsport.TIRE_COMPOUND, SpecRegistries.F1_SLICK_FRONT.location());
        }
        overrides = onModelChange(overrides);
        if (overrides.isEmpty()) {
            stack.remove(CreateMotorsport.TIRE_COMPOUND_OVERRIDES);
        } else {
            stack.set(CreateMotorsport.TIRE_COMPOUND_OVERRIDES, overrides);
        }
    }

    private CompoundTag onModelChange(CompoundTag overrides) {
        TireSpec pack = SpecRegistries.fittedCompound(access, stack, true);
        CompoundTag current = stack.getOrDefault(CreateMotorsport.TIRE_COMPOUND_OVERRIDES, new CompoundTag());
        TireSpec.Model before = SpecOverrides.apply(SpecCodecs.TIRE_SPEC, pack, current).model();
        TireSpec.Model after = SpecOverrides.apply(SpecCodecs.TIRE_SPEC, pack, overrides).model();
        if (before == after) {
            return overrides;
        }
        CompoundTag kept = overrides.copy();
        for (String field : SpecCodecs.modelFields(before)) {
            kept.remove(field);
        }
        if (after == pack.model()) {
            kept.remove("model");
        }
        return kept;
    }

    private void cycleCompound(int step) {
        ResourceLocation current = stack.get(CreateMotorsport.TIRE_COMPOUND);
        ResourceKey<TireSpec> next = SpecRegistries.cycle(access, SpecRegistries.TIRE_COMPOUND,
                current == null ? null : ResourceKey.create(SpecRegistries.TIRE_COMPOUND, current), List.of(), step);
        if (next != null) {
            stack.set(CreateMotorsport.TIRE_COMPOUND, next.location());
            stack.remove(CreateMotorsport.TIRE_COMPOUND_OVERRIDES);
        }
    }
}
