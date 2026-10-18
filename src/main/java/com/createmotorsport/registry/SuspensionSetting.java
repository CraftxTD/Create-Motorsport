package com.createmotorsport.registry;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.physics.spec.SuspensionSpec;
import net.minecraft.world.level.block.state.BlockState;

public enum SuspensionSetting {
    SOFT(SpecRegistries.SOFT, SuspensionSpec.SOFT),
    MEDIUM(SpecRegistries.MEDIUM, SuspensionSpec.MEDIUM),
    FIRM(SpecRegistries.FIRM, SuspensionSpec.FIRM),
    RACE(SpecRegistries.RACE, SuspensionSpec.RACE),
    RACE_REAR(SpecRegistries.RACE_REAR, SuspensionSpec.RACE_REAR),
    PASSENGER_STRUT(SpecRegistries.PASSENGER_STRUT, SuspensionSpec.PASSENGER_STRUT),
    TRUCK_STRUT(SpecRegistries.TRUCK_STRUT, SuspensionSpec.TRUCK_STRUT);

    private final net.minecraft.resources.ResourceKey<SuspensionSpec> key;
    private final SuspensionSpec fallback;

    SuspensionSetting(net.minecraft.resources.ResourceKey<SuspensionSpec> key, SuspensionSpec fallback) {
        this.key = key;
        this.fallback = fallback;
    }

    public net.minecraft.resources.ResourceKey<SuspensionSpec> key() {
        return this.key;
    }

    public static SuspensionSpec fallbackFor(net.minecraft.resources.ResourceKey<SuspensionSpec> key) {
        for (SuspensionSetting s : values()) {
            if (s.key == key) {
                return s.fallback;
            }
        }
        return SuspensionSpec.RACE;
    }

    public static net.minecraft.resources.ResourceKey<SuspensionSpec> defaultFor(BlockState state) {
        if (state.is(CreateMotorsport.PASSENGER_SUSPENSION.get())) {
            return SpecRegistries.PASSENGER_STRUT;
        }
        return state.is(CreateMotorsport.TRUCK_SUSPENSION.get())
                ? SpecRegistries.TRUCK_STRUT : SpecRegistries.RACE;
    }
}
