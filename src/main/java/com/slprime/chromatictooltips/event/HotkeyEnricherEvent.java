package com.slprime.chromatictooltips.event;

import java.util.Map;

import com.slprime.chromatictooltips.api.TooltipContext;

public class HotkeyEnricherEvent extends TooltipEvent {

    public Map<String, String> hotkeys;

    public HotkeyEnricherEvent(TooltipContext context, Map<String, String> hotkeys) {
        super(context);
        this.hotkeys = hotkeys;
    }

}
