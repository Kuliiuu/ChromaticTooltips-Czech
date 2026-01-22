package com.slprime.chromatictooltips.event;

import com.slprime.chromatictooltips.api.TooltipContext;

public class ModInfoEnricherEvent extends TooltipEvent {

    public String modName;
    public String modId;
    public String itemId;

    public ModInfoEnricherEvent(TooltipContext context, String modName, String modId, String itemId) {
        super(context);
        this.modName = modName;
        this.modId = modId;
        this.itemId = itemId;
    }

}
