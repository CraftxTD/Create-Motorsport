package com.createmotorsport.client.screen.part;

import com.createmotorsport.block.entity.SuspensionBlockEntity;
import com.createmotorsport.client.widget.Theme;
import com.createmotorsport.network.ToggleAxlePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class SuspensionPartScreen extends PartScreen {

    public SuspensionPartScreen(BlockPos pos, boolean canTune) {
        super(Component.translatable("container.createmotorsport.suspension"), pos, canTune);
    }

    private Button toggle(int setting, Function<SuspensionBlockEntity, String> label, boolean enabled) {
        return new Button(() -> {
            SuspensionBlockEntity axle = blockEntity(SuspensionBlockEntity.class);
            return Component.literal(axle == null ? "--" : label.apply(axle));
        }, Theme.NEUTRAL, () -> PacketDistributor.sendToServer(new ToggleAxlePacket(pos, setting)), enabled);
    }

    @Override
    protected List<List<Button>> rows() {
        return List.of(
                List.of(tune()),
                List.of(toggle(ToggleAxlePacket.STEERING, a -> "Steering: " + yes(a.isSteered()), true),
                        toggle(ToggleAxlePacket.DRIVEN, a -> "Driven: " + yes(a.isDriven()), canTune)),
                List.of(toggle(ToggleAxlePacket.END, a -> "End: " + (a.isFrontAxle() ? "Front" : "Rear"), true)),
                List.of(wireless()));
    }

    private static String yes(boolean on) {
        return on ? "True" : "False";
    }
}
