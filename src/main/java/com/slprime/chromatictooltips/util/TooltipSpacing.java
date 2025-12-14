package com.slprime.chromatictooltips.util;

public class TooltipSpacing {

    public static final TooltipSpacing ZERO = new TooltipSpacing(new int[] { 0, 0, 0, 0 });

    protected int left;
    protected int right;
    protected int top;
    protected int bottom;

    public TooltipSpacing(int top, int right, int bottom, int left) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public TooltipSpacing(int[] values) {
        this.top = values[0];
        this.right = values[1];
        this.bottom = values[2];
        this.left = values[3];
    }

    public int getLeft() {
        return this.left;
    }

    public int getRight() {
        return this.right;
    }

    public int getTop() {
        return this.top;
    }

    public int getBottom() {
        return this.bottom;
    }

    public int getInline() {
        return this.left + this.right;
    }

    public int getBlock() {
        return this.top + this.bottom;
    }

    public String toString() {
        return "Spacing{left=" + left + ", right=" + right + ", top=" + top + ", bottom=" + bottom + '}';
    }

}
