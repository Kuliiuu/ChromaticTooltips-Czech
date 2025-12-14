package com.slprime.chromatictooltips.util;

public enum TooltipAlign {

    START,
    CENTER,
    END;

    public static TooltipAlign fromString(String str) {
        switch (str.toLowerCase()) {
            case "start":
            case "left":
            case "top":
                return START;
            case "center":
                return CENTER;
            case "end":
            case "right":
            case "bottom":
                return END;
            default:
                return START;
        }
    }

}
