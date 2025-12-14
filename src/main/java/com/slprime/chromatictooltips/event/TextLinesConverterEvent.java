package com.slprime.chromatictooltips.event;

import java.util.ArrayList;
import java.util.List;

import com.slprime.chromatictooltips.api.TooltipContext;

public class TextLinesConverterEvent extends TooltipEvent {

    public List<Object> list;

    public TextLinesConverterEvent(TooltipContext context, List<?> list) {
        super(context);
        this.list = new ArrayList<>(list);
    }

}
