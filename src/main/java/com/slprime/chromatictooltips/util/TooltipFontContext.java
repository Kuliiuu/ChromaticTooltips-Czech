package com.slprime.chromatictooltips.util;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.slprime.chromatictooltips.api.TooltipStyle;

public class TooltipFontContext {

    protected static class Context {

        public int defaultColor = INHERIT_COLOR;
        public boolean shadow = true;

        public Context(int defaultColor, boolean shadow) {
            this.defaultColor = defaultColor;
            this.shadow = shadow;
        }
    }

    public static final int INHERIT_COLOR = 0x00000000;

    protected static int[] previousColors = null;
    protected static FontRenderer fontRenderer = null;
    protected static Context staticContext = new Context(0xffffffff, true);
    protected static final String[] colorOrders = new String[] { "black", "dark_blue", "dark_green", "dark_aqua",
        "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
        "yellow", "white" };

    protected Context previousContext = null;
    protected int[] customColors = new int[32];

    protected int defaultColor = INHERIT_COLOR;
    protected Boolean shadow = null;

    public TooltipFontContext(TooltipStyle formatting) {
        final TooltipStyle colorStyle = new TooltipStyle(
            formatting.getAsJsonObject("font.colors", formatting.getAsJsonObject("fontColors", new JsonObject())));

        this.defaultColor = formatting
            .getAsColor("font.defaultColor", formatting.getAsColor("defaultColor", this.defaultColor));

        if (formatting.containsKey("font.shadow")) {
            this.shadow = formatting.getAsBoolean("font.shadow", true);
        } else if (formatting.containsKey("fontShadow")) {
            this.shadow = formatting.getAsBoolean("fontShadow", true);
        }

        Arrays.fill(this.customColors, INHERIT_COLOR);

        for (int i = 0; i < colorOrders.length; i++) {
            final String key = colorOrders[i];

            if (colorStyle.containsKey(key)) {
                final JsonElement element = colorStyle.get(key);

                if (element.isJsonArray()) {
                    final int[] colors = colorStyle.getAsColors(key, new int[] { INHERIT_COLOR });

                    this.customColors[i] = colors[0];

                    if (colors.length == 1) {
                        this.customColors[i + 16] = (colors[0] & 16579836) >> 2 | colors[0] & -16777216;
                    } else {
                        this.customColors[i + 16] = colors[1];
                    }

                } else if (element.isJsonPrimitive()) {
                    final int color = colorStyle.getAsColor(key, INHERIT_COLOR);

                    this.customColors[i] = color;
                    this.customColors[i + 16] = (color & 16579836) >> 2 | color & -16777216;
                }

            }

        }

    }

    public void pushContext() {
        this.previousContext = TooltipFontContext.staticContext;

        TooltipFontContext.staticContext = new Context(
            this.defaultColor == INHERIT_COLOR ? TooltipFontContext.staticContext.defaultColor : this.defaultColor,
            this.shadow == null ? TooltipFontContext.staticContext.shadow : this.shadow);

        if (previousColors == null) {
            previousColors = Arrays.copyOf(getFontRenderer().colorCode, 32);
        }

        for (int i = 0; i < 32; i++) {
            if (this.customColors[i] != INHERIT_COLOR) {
                getFontRenderer().colorCode[i] = this.customColors[i];
            }
        }
    }

    public void popContext() {

        if (previousColors != null) {
            System.arraycopy(previousColors, 0, getFontRenderer().colorCode, 0, 32);
        }

        TooltipFontContext.staticContext = this.previousContext;
    }

    public static FontRenderer getFontRenderer() {

        if (TooltipFontContext.fontRenderer == null) {
            TooltipFontContext.fontRenderer = ClientUtil.mc().fontRenderer;
        }

        return TooltipFontContext.fontRenderer;
    }

    public static int getStringWidth(String text) {
        return getFontRenderer().getStringWidth(text);
    }

    public static List<String> listFormattedStringToWidth(String text, int maxWidth) {
        return getFontRenderer().listFormattedStringToWidth(text, maxWidth);
    }

    public static void drawString(String text, int x, int y) {
        drawString(text, x, y, TooltipFontContext.staticContext.defaultColor);
    }

    public static void drawString(String text, int x, int y, int color) {

        if (color == TooltipFontContext.INHERIT_COLOR) {
            color = TooltipFontContext.staticContext.defaultColor;
        }

        getFontRenderer().drawString(text, x, y, color, TooltipFontContext.staticContext.shadow);
    }

}
