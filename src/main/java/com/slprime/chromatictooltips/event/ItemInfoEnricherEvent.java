package com.slprime.chromatictooltips.event;

import java.util.ArrayList;
import java.util.List;

import com.slprime.chromatictooltips.api.TooltipContext;

public class ItemInfoEnricherEvent extends TooltipEvent {

    public List<Object> lines;

    public ItemInfoEnricherEvent(TooltipContext context, List<?> lines) {
        super(context);
        this.lines = new ArrayList<>(lines);
    }

}
