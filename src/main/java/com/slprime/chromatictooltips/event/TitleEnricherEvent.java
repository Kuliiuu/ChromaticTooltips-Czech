package com.slprime.chromatictooltips.event;

import com.slprime.chromatictooltips.api.TooltipContext;

public class TitleEnricherEvent extends TooltipEvent {

    public String displayName;

    public TitleEnricherEvent(TooltipContext context, String displayName) {
        super(context);
        this.displayName = displayName;
    }

}
