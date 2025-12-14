package com.slprime.chromatictooltips.event;

import java.awt.Point;

import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.component.SectionTooltipComponent;
import com.slprime.chromatictooltips.util.TooltipSpacing;

public class RenderTooltipEvent extends TooltipEvent {

    public final SectionTooltipComponent activePage;
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public RenderTooltipEvent(TooltipContext context, SectionTooltipComponent activePage, Point position) {
        super(context);
        this.activePage = activePage;

        final TooltipSpacing margin = activePage.getMargin();

        this.x = position.x + margin.getLeft();
        this.y = position.y + margin.getTop();
        this.width = activePage.getWidth() - margin.getInline();
        this.height = activePage.getHeight() - margin.getBlock();
    }

}
