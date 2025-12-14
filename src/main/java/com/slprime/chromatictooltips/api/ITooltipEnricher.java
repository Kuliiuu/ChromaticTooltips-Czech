package com.slprime.chromatictooltips.api;

import java.util.List;

public interface ITooltipEnricher {

    public List<ITooltipComponent> enrich(TooltipContext context);

}
