package com.slprime.chromatictooltips.event;

import java.util.ArrayList;
import java.util.List;

import com.slprime.chromatictooltips.api.TooltipContext;

public class ItemInfoEnricherEvent extends TooltipEvent {

    public List<Object> tooltip;

    public ItemInfoEnricherEvent(TooltipContext context, List<?> tooltip) {
        super(context);
        this.tooltip = new ArrayList<>(tooltip);
    }

}
