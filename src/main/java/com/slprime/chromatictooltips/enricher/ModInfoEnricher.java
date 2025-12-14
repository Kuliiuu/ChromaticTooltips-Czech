package com.slprime.chromatictooltips.enricher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.slprime.chromatictooltips.api.ITooltipComponent;
import com.slprime.chromatictooltips.api.ITooltipEnricher;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.api.TooltipStyle;
import com.slprime.chromatictooltips.component.TextTooltipComponent;
import com.slprime.chromatictooltips.util.TooltipFontContext;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;

public class ModInfoEnricher implements ITooltipEnricher {

    protected static class ModNameTooltipComponent extends TextTooltipComponent {

        protected String modname;
        protected int modnameWidth;

        protected String identifier;
        protected int identifierColor;

        protected int delimiterWidth;
        protected int delimiterColor;

        public ModNameTooltipComponent(String modname, String identifier, int modnameColor, int delimiterColor,
            int identifierColor) {
            super(modname + ":" + identifier, modnameColor);

            this.modname = modname;
            this.modnameWidth = TooltipFontContext.getStringWidth(modname);

            this.delimiterWidth = TooltipFontContext.getStringWidth(":");
            this.delimiterColor = delimiterColor;

            this.identifier = identifier;
            this.identifierColor = identifierColor;
        }

        @Override
        public ITooltipComponent[] paginate(int maxWidth, int maxHeight) {
            return new ITooltipComponent[] { this };
        }

        @Override
        public void draw(int x, int y, int availableWidth, TooltipContext context) {
            context.drawString(this.modname, x, y, this.color);
            context.drawString(":", x + this.modnameWidth, y, this.delimiterColor);
            context.drawString(this.identifier, x + this.modnameWidth + this.delimiterWidth, y, this.identifierColor);
        }

    }

    protected static final String DEFAULT_MOD_NAME = "Minecraft";
    protected static final UniqueIdentifier UNKNOWN_IDENTIFIER = new UniqueIdentifier("Unknown:unknown");

    protected static volatile Map<String, ModContainer> namedMods = null;

    @Override
    public List<ITooltipComponent> enrich(TooltipContext context) {
        final ItemStack stack = context.getStack();

        if (stack == null) {
            return null;
        }

        final List<ITooltipComponent> components = new ArrayList<>();
        final TooltipStyle modnameStyle = context.getAsStyle("modInfo");
        final boolean compact = modnameStyle.getAsBoolean("compact", true);
        final UniqueIdentifier identifier = getIdentifier(stack);
        final String modname = nameFromStack(identifier);

        if (compact && modname.replaceAll("\\s+", "")
            .equalsIgnoreCase(identifier.modId.replaceAll("\\s+", ""))) {
            components.add(
                new ModNameTooltipComponent(
                    modname,
                    identifier.name,
                    modnameStyle.getAsColor("modnameColor", 0xffAA00AA),
                    modnameStyle.getAsColor("delimiterColor", 0xffAA00AA),
                    modnameStyle.getAsColor("identifierColor", 0xff808080)));
        } else {
            components.add(
                new TextTooltipComponent(
                    identifier.toString(),
                    modnameStyle.getAsColor("identifierColor", 0xff808080)));
            components.add(new TextTooltipComponent(modname, modnameStyle.getAsColor("modnameColor", 0xffAA00AA)));
        }

        return components;
    }

    protected static UniqueIdentifier getIdentifier(ItemStack stack) {
        final UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        return identifier != null ? identifier : UNKNOWN_IDENTIFIER;
    }

    protected static String nameFromStack(UniqueIdentifier identifier) {

        if (namedMods == null) {
            namedMods = Loader.instance()
                .getIndexedModList();
        }

        final ModContainer modContainer = namedMods.get(identifier.modId);
        return modContainer != null ? modContainer.getName() : DEFAULT_MOD_NAME;
    }

}
