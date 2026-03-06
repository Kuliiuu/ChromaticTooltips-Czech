package com.slprime.chromatictooltips.event;

import com.slprime.chromatictooltips.api.TooltipTarget;

public class FoodStatsEvent extends TooltipEvent {

    public int hunger = 0;
    public float saturationModifier = 0.0F;

    public FoodStatsEvent(TooltipTarget target) {
        super(target);
    }

}
