package com.slprime.chromatictooltips.event;

import java.util.List;

import com.slprime.chromatictooltips.api.ItemStats;
import com.slprime.chromatictooltips.api.TooltipContext;

public class AttributeEnricherEvent extends TooltipEvent {

    public final List<ItemStats> stats;

    public AttributeEnricherEvent(TooltipContext context, List<ItemStats> stats) {
        super(context);
        this.stats = stats;
    }

}
