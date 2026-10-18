package com.createmotorsport.client.screen;

import com.createmotorsport.client.screen.tune.TuneScreen;
import com.createmotorsport.client.screen.tune.TuneSource;
import com.createmotorsport.item.HeldTire;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TireScreenOpener {

    private TireScreenOpener() {
    }

    public static void open(InteractionHand hand, boolean canTune) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = mc.player == null ? ItemStack.EMPTY : mc.player.getItemInHand(hand);
        if (HeldTire.tunable(stack)) {
            mc.setScreen(new TuneScreen(null, stack.getHoverName(), TuneSource.held(hand), canTune));
        }
    }
}
