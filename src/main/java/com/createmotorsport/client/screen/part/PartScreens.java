package com.createmotorsport.client.screen.part;

import com.createmotorsport.menu.PartKind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class PartScreens {

    private static BlockPos statusPos;
    private static List<Component> statusLines = List.of();

    private PartScreens() {
    }

    public static void open(BlockPos pos, PartKind kind, boolean canTune) {
        statusPos = pos;
        statusLines = List.of();
        Minecraft.getInstance().setScreen(switch (kind) {
            case ENGINE -> new EnginePartScreen(pos, canTune);
            case SUSPENSION -> new SuspensionPartScreen(pos, canTune);
            case STEERING_WHEEL -> new SteeringWheelPartScreen(pos, canTune);
        });
    }

    public static void status(BlockPos pos, List<Component> lines) {
        if (pos.equals(statusPos)) {
            statusLines = lines;
        }
    }

    static List<Component> statusFor(BlockPos pos) {
        return pos.equals(statusPos) ? statusLines : List.of();
    }
}
