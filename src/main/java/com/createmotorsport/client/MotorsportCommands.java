package com.createmotorsport.client;

import com.createmotorsport.network.StartTelemetryLogPacket;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;


// Client commands. Currently just (/motorsports log [seconds] [samples per second])
public final class MotorsportCommands {
    private MotorsportCommands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("motorsports")
                        .then(Commands.literal("log")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                        .executes(ctx -> startLog(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "seconds"), 2))
                                        .then(Commands.argument("samplesPerSec", IntegerArgumentType.integer(1, 20))
                                                .executes(ctx -> startLog(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "seconds"),
                                                        IntegerArgumentType.getInteger(ctx, "samplesPerSec")))))));
    }

    private static int startLog(net.minecraft.commands.CommandSourceStack source, int seconds, int samplesPerSec) {
        if (!SteeringInputHandler.isDriving()) {
            source.sendFailure(Component.literal("You must be driving a car to log data"));
            return 0;
        }
        PacketDistributor.sendToServer(new StartTelemetryLogPacket(seconds, samplesPerSec));
        source.sendSystemMessage(Component.literal(
                "§e[Motorsports] Requested a " + seconds + "s telemetry log ("
                        + samplesPerSec + " samples/s)"));
        return 1;
    }
}
