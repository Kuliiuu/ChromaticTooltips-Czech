package com.slprime.chromatictooltips.event;

import net.minecraftforge.fluids.Fluid;

import com.slprime.chromatictooltips.api.TooltipContext;

public class StackSizeEnricherEvent extends TooltipEvent {

    public long stackAmount;

    public Fluid containedFluid;
    public long containedFluidAmount;

    public StackSizeEnricherEvent(TooltipContext context, long stackAmount, Fluid containedFluid,
        long containedFluidAmount) {
        super(context);
        this.stackAmount = stackAmount;
        this.containedFluid = containedFluid;
        this.containedFluidAmount = containedFluidAmount;
    }

}
