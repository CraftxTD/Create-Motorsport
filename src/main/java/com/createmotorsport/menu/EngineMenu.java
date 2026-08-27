package com.createmotorsport.menu;

import com.createmotorsport.CreateMotorsport;
import com.createmotorsport.block.entity.EngineBlockEntity;
import com.createmotorsport.block.entity.EngineBlockEntity.ControlChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Again just a temporary menu because I needed one working, so based on my keyboard mods style
public class EngineMenu extends AbstractContainerMenu {
    private static final int ENGINE_SLOT_COUNT = EngineBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = ENGINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    public static final int COMPONENT_Y = 44;
    public static final int EXHAUST_X = 72;
    public static final int INTAKE_X = 98;
    public static final int CHANNEL_Y0 = 70;
    public static final int CHANNEL_ROW_H = 22;
    public static final int CHANNEL_SLOT_A_X = 120;
    public static final int CHANNEL_SLOT_B_X = 142;
    public static final int INV_X = 20;


    public static final int INV_Y = CHANNEL_Y0 + EngineBlockEntity.CHANNELS.length * CHANNEL_ROW_H + 12;
    public static final int HOTBAR_Y = INV_Y + 58;

    // Readout easily can be synced here since I'm displaying on the dashboard anyway
    private static final int DATA_RPM = 0;
    private static final int DATA_GEAR = 1;
    private static final int DATA_COUNT = 2;

    private final Container engineInventory;
    private final ContainerData data;
    private final BlockPos enginePos;

    public EngineMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, new SimpleContainer(ENGINE_SLOT_COUNT), null, pos);
    }

    public EngineMenu(int containerId, Inventory playerInventory, EngineBlockEntity engine) {
        this(containerId, playerInventory, engine.getInventory(), engine, engine.getBlockPos());
    }

    private EngineMenu(int containerId, Inventory playerInventory, Container engineInventory, EngineBlockEntity engine,
                       BlockPos pos) {
        super(CreateMotorsport.ENGINE_MENU.get(), containerId);
        this.enginePos = pos;
        checkContainerSize(engineInventory, ENGINE_SLOT_COUNT);
        this.engineInventory = engineInventory;
        this.data = engine != null ? new ContainerData() {
            @Override
            public int get(int index) {
                return index == DATA_RPM ? engine.getDisplayRpm() : engine.getGearCode();
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        } : new SimpleContainerData(DATA_COUNT);
        engineInventory.startOpen(playerInventory.player);

        addSlot(new Slot(engineInventory, EngineBlockEntity.SLOT_EXHAUST, EXHAUST_X, COMPONENT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(CreateMotorsport.EXHAUST_MANIFOLD.get());
            }
        });
        addSlot(new Slot(engineInventory, EngineBlockEntity.SLOT_INTAKE, INTAKE_X, COMPONENT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(CreateMotorsport.AIR_INTAKE.get());
            }
        });

        for (ControlChannel channel : EngineBlockEntity.CHANNELS) {
            int y = CHANNEL_Y0 + channel.ordinal() * CHANNEL_ROW_H;
            addSlot(new FrequencySlot(engineInventory, EngineBlockEntity.channelSlotA(channel), CHANNEL_SLOT_A_X, y));
            addSlot(new FrequencySlot(engineInventory, EngineBlockEntity.channelSlotB(channel), CHANNEL_SLOT_B_X, y));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, INV_X + column * 18, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    public BlockPos getEnginePos() {
        return enginePos;
    }

    public int getRpm() {
        return data.get(DATA_RPM);
    }

    public int getGearCode() {
        return data.get(DATA_GEAR);
    }

    // Item slots for Redstone links
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
        return engineInventory.stillValid(player);
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

        if (index < ENGINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, ENGINE_SLOT_COUNT, false)) {
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
        engineInventory.stopOpen(player);
    }
}
