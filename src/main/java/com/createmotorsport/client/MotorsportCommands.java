package com.createmotorsport.client;

import com.createmotorsport.network.StartTelemetryLogPacket;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;


// Client commands. Currently just (/motorsports log [seconds])
public final class MotorsportCommands {
    private MotorsportCommands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("motorsports")
                        .then(Commands.literal("log")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                        .executes(ctx -> {
                                            int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                            if (!SteeringInputHandler.isDriving()) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "You must be driving a car to log data"));
                                                return 0;
                                            }
                                            PacketDistributor.sendToServer(new StartTelemetryLogPacket(seconds));
                                            ctx.getSource().sendSystemMessage(Component.literal(
                                                    "§e[Motorsports] Requested a " + seconds
                                                            + "s telemetry log (2 samples/s)"));
                                            return 1;
                                        }))));
    }
}
