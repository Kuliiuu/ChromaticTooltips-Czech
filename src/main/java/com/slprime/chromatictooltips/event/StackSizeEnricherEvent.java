package com.slprime.chromatictooltips.event;

import net.minecraftforge.fluids.FluidStack;

import com.slprime.chromatictooltips.api.TooltipContext;

public class StackSizeEnricherEvent extends TooltipEvent {

    public FluidStack fluid;
    public long stackSize;

    public StackSizeEnricherEvent(TooltipContext context, FluidStack fluid, long stackSize) {
        super(context);
        this.fluid = fluid;
        this.stackSize = stackSize;
    }

}
