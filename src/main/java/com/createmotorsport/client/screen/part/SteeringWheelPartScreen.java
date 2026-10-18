package com.createmotorsport.client.screen.part;

import com.createmotorsport.block.entity.SteeringWheelBlockEntity;
import com.createmotorsport.block.entity.SteeringWheelBlockEntity.Aid;
import com.createmotorsport.client.input.SteeringInputHandler;
import com.createmotorsport.client.widget.Theme;
import com.createmotorsport.network.SetCarAidPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class SteeringWheelPartScreen extends PartScreen {

    public SteeringWheelPartScreen(BlockPos pos, boolean canTune) {
        super(Component.translatable("container.createmotorsport.steering_wheel"), pos, canTune);
    }

    private boolean driving() {
        return SteeringInputHandler.isDriving() && pos.equals(SteeringInputHandler.getActivePos());
    }

    private Button aid(Aid aid, Function<SteeringWheelBlockEntity, String> label) {
        return new Button(() -> {
            SteeringWheelBlockEntity wheel = blockEntity(SteeringWheelBlockEntity.class);
            return Component.literal(wheel == null ? "--" : label.apply(wheel));
        }, Theme.NEUTRAL, () -> PacketDistributor.sendToServer(new SetCarAidPacket(pos, aid)));
    }

    @Override
    protected List<List<Button>> rows() {
        Button drive = new Button(() -> Component.literal(driving() ? "Stop Driving" : "Start Driving"),
                Theme.GO, () -> {
                    if (driving()) {
                        SteeringInputHandler.stop();
                    } else {
                        SteeringInputHandler.startDriving(pos);
                        onClose();
                    }
                });
        return List.of(
                List.of(new Button(() -> Component.literal("Keybinds"), Theme.ACCENT,
                        () -> minecraft.setScreen(new SteeringKeysScreen(this, pos)))),
                List.of(drive),
                List.of(aid(Aid.STEER_ASSIST, w -> "Steer: " + (w.isSteerAssistOff() ? "Off" : "On"))
                                .withTip("Steering Assist: recommended for keyboard users"),
                        aid(Aid.DIFF_MODE, w -> "Diff: " + (w.isDiffModeOn() ? "On" : "Off"))
                                .withTip("Diff Mode: toggle between two differential settings to aid drifting"),
                        aid(Aid.TRACTION_CONTROL, w -> "TC: " + (w.isTractionControlOn() ? "On" : "Off"))
                                .withTip("Traction Control: recommended for keyboard users")),
                List.of(aid(Aid.TERRAIN_ASSIST, w -> "Terrain: " + w.getTerrainAssist().label())
                        .withTip("Terrain Assist: needs a lot of work. It pretends there is fake ramps under the "
                                + "wheels, because realistic suspension and 1 meter tall bumps don't work together")),
                List.of(wireless()));
    }
}
