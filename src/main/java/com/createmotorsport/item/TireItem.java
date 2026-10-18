package com.createmotorsport.item;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.network.OpenTuneHeldPacket;
import com.createmotorsport.network.TunePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;


public class TireItem extends Item {

    public TireItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation tire = stack.get(CreateMotorsport.TIRE);
        if (tire != null) {
            return Component.translatable("tire." + tire.getNamespace() + "." + tire.getPath());
        }
        ResourceLocation compound = stack.get(CreateMotorsport.TIRE_COMPOUND);
        if (compound == null) {
            return super.getName(stack);
        }
        return Component.translatable("tire_compound." + compound.getNamespace() + "." + compound.getPath());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!HeldTire.tunable(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer server) {
            PacketDistributor.sendToPlayer(server,
                    new OpenTuneHeldPacket(hand == InteractionHand.MAIN_HAND, TunePacket.mayTune(server)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
