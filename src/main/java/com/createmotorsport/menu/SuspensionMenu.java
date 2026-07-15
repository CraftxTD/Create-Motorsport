package com.createmotorsport.menu;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.SuspensionBlockEntity;
import com.createmotorsport.block.entity.SuspensionBlockEntity.SteerChannel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;


// Menu for the redstone links on suspension
// Temporary, but I've made a ton of these in this style so I could just do this quickly to have something working

public class SuspensionMenu extends AbstractContainerMenu {
    private static final int CONTROL_SLOT_COUNT = SuspensionBlockEntity.CONTROL_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = CONTROL_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    public static final int CHANNEL_Y0 = 40;
    public static final int CHANNEL_ROW_H = 20;
    public static final int CHANNEL_SLOT_A_X = 120;
    public static final int CHANNEL_SLOT_B_X = 142;
    public static final int INV_X = 20;
    public static final int INV_Y = 110;
    public static final int HOTBAR_Y = INV_Y + 58;

    private final Container controls;

    public SuspensionMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CONTROL_SLOT_COUNT), null);
    }

    public SuspensionMenu(int containerId, Inventory playerInventory, SuspensionBlockEntity suspension) {
        this(containerId, playerInventory, suspension.getControls(), suspension);
    }

    private SuspensionMenu(int containerId, Inventory playerInventory, Container controls, SuspensionBlockEntity suspension) {
        super(CreateMotorsport.SUSPENSION_MENU.get(), containerId);
        checkContainerSize(controls, CONTROL_SLOT_COUNT);
        this.controls = controls;
        controls.startOpen(playerInventory.player);

        for (SteerChannel channel : SuspensionBlockEntity.CHANNELS) {
            int y = CHANNEL_Y0 + channel.ordinal() * CHANNEL_ROW_H;
            addSlot(new FrequencySlot(controls, SuspensionBlockEntity.channelSlotA(channel), CHANNEL_SLOT_A_X, y));
            addSlot(new FrequencySlot(controls, SuspensionBlockEntity.channelSlotB(channel), CHANNEL_SLOT_B_X, y));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, INV_X + column * 18, HOTBAR_Y));
        }
    }

    private static class FrequencySlot extends Slot {
        FrequencySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return controls.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < CONTROL_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, CONTROL_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        controls.stopOpen(player);
    }
}
