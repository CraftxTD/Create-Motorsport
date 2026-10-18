package com.createmotorsport.menu;

import com.createmotorsport.block.entity.EngineBlockEntity;
import com.createmotorsport.block.entity.SteeringWheelBlockEntity;
import com.createmotorsport.block.entity.SuspensionBlockEntity;
import com.createmotorsport.network.OpenPartScreenPacket;
import com.createmotorsport.network.TunePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PartMenus {

    private PartMenus() {
    }

    public static boolean openPartScreen(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof PartPanel panel)) {
            return false;
        }
        PacketDistributor.sendToPlayer(player, new OpenPartScreenPacket(pos, panel.partKind(),
                TunePacket.mayTune(player)));
        return true;
    }

    public static void openWireless(ServerPlayer player, BlockPos pos) {
        boolean canTune = TunePacket.mayTune(player);
        var be = player.level().getBlockEntity(pos);
        Component title = Component.translatable("container.createmotorsport.wireless");
        if (be instanceof EngineBlockEntity engine) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new EngineMenu(id, inv, engine), title), buf -> {
                buf.writeBlockPos(pos);
                buf.writeBoolean(canTune);
            });
        } else if (be instanceof SuspensionBlockEntity suspension) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SuspensionMenu(id, inv, suspension), title),
                    buf -> {
                        buf.writeBlockPos(pos);
                        buf.writeBoolean(canTune);
                    });
        } else if (be instanceof SteeringWheelBlockEntity wheel) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SteeringWheelMenu(id, inv, wheel), title), pos);
        }
    }
}
