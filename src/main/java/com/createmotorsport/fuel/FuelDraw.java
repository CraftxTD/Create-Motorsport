package com.createmotorsport.fuel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class FuelDraw {

    private FuelDraw() {
    }

    public static boolean drain(Level level, BlockPos pos, int amount) {
        for (Direction direction : Direction.values()) {
            IFluidHandler handler = FluidUtil.getFluidHandler(level, pos.relative(direction),
                    direction.getOpposite()).orElse(null);
            if (handler == null) continue;
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fuel = handler.getFluidInTank(i).copyWithAmount(amount);
                if (!FuelRules.accepts(fuel)) continue;
                FluidStack simulated = handler.drain(fuel, IFluidHandler.FluidAction.SIMULATE);
                if (simulated.getAmount() != amount || !FluidStack.isSameFluidSameComponents(fuel, simulated)) continue;
                FluidStack drained = handler.drain(fuel, IFluidHandler.FluidAction.EXECUTE);
                if (drained.getAmount() == amount && FluidStack.isSameFluidSameComponents(fuel, drained)) return true;
                if (!drained.isEmpty()) handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            }
        }
        return false;
    }

    public static int inReach(Level level, BlockPos pos) {
        int total = 0;
        for (Direction direction : Direction.values()) {
            IFluidHandler handler = FluidUtil.getFluidHandler(level, pos.relative(direction),
                    direction.getOpposite()).orElse(null);
            if (handler == null) continue;
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fluid = handler.getFluidInTank(i);
                if (FuelRules.accepts(fluid)) {
                    total += fluid.getAmount();
                }
            }
        }
        return total;
    }
}
