package com.slprime.chromatictooltips;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import net.minecraft.client.resources.IResource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.slprime.chromatictooltips.api.ITooltipComponent;
import com.slprime.chromatictooltips.api.ITooltipEnricher;
import com.slprime.chromatictooltips.api.ITooltipRenderer;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.api.TooltipStyle;
import com.slprime.chromatictooltips.component.TextTooltipComponent;
import com.slprime.chromatictooltips.event.TextLinesConverterEvent;
import com.slprime.chromatictooltips.event.TooltipEnricherEvent;
import com.slprime.chromatictooltips.util.ClientUtil;
import com.slprime.chromatictooltips.util.Parser;

public class TooltipHandler {

    protected static final WeakHashMap<ITooltipComponent, String> tipLineComponents = new WeakHashMap<>();
    protected static int nextComponentId = 0;

    protected static final TooltipHandler instance = new TooltipHandler();
    protected static final String CONFIG_FILE = "tooltip.json";
    protected static final String COMPONENT_PREFIX = "\u00A7z";

    protected ITooltipRenderer defaultTooltipRenderer = null;
    protected Map<String, List<ITooltipRenderer>> otherTooltipRenderers = new HashMap<>();

    protected Class<? extends ITooltipRenderer> rendererClass = null;
    protected final Map<String, ITooltipEnricher> tooltipEnrichers = new LinkedHashMap<>();

    // cache
    protected TooltipContext lastContext = null;
    protected List<?> lastTextLines = null;
    protected boolean ignoreLastTooltip = true;
    protected int lastHashMode = 0;

    protected TooltipHandler() {
        reload();
    }

    public static TooltipHandler instance() {
        return instance;
    }

    public void reload() {
        this.otherTooltipRenderers.clear();
        this.defaultTooltipRenderer = null;

        loadTooltipResource(CONFIG_FILE);

        if (this.defaultTooltipRenderer == null) {
            this.defaultTooltipRenderer = getRenderer(new TooltipStyle());
        }
    }

    protected void parseStyle(String json) {
        final List<TooltipStyle> scopes = (new Parser()).parse(json);

        for (TooltipStyle style : scopes) {
            String context = style.getAsString("context", null);

            if (context == null && style.containsKey("filter")) {
                context = "item";
            }

            if (context == null || "default".equals(context)) {
                this.defaultTooltipRenderer = getRenderer(style);
            } else {
                this.otherTooltipRenderers.computeIfAbsent(context, k -> new ArrayList<>())
                    .add(getRenderer(style));
            }

        }

    }

    protected boolean loadTooltipResource(String resource) {
        final ResourceLocation location = new ResourceLocation(ChromaticTooltips.MODID, resource);

        try {
            final IResource res = ClientUtil.mc()
                .getResourceManager()
                .getResource(location);

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                ChromaticTooltips.LOG.info("Loading '{}' from resourcepack {}", resource, location);
                parseStyle(
                    reader.lines()
                        .collect(Collectors.joining("\n")));
                return true;
            }

        } catch (Exception io) {
            ChromaticTooltips.LOG.error("Failed to load '{}' resourcepack {}", resource, location);
        }

        return false;
    }

    protected ITooltipRenderer getRenderer(TooltipStyle style) {
        try {
            return this.rendererClass.getConstructor(TooltipStyle.class)
                .newInstance(style);
        } catch (Exception e1) {
            return new TooltipRenderer(style);
        }
    }

    public static String getComponentId(ITooltipComponent component) {
        TooltipHandler.tipLineComponents
            .put(component, TooltipHandler.COMPONENT_PREFIX + TooltipHandler.nextComponentId++);
        return TooltipHandler.tipLineComponents.get(component);
    }

    public static ITooltipComponent getTooltipComponent(String line) {
        if (!line.startsWith(TooltipHandler.COMPONENT_PREFIX)) return null;
        return TooltipHandler.tipLineComponents.entrySet()
            .stream()
            .filter(
                entry -> entry.getValue()
                    .equals(line))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    public void addEnricher(String id, ITooltipEnricher enricher) {
        this.tooltipEnrichers.put(id, enricher);
    }

    public void setRendererClass(Class<? extends ITooltipRenderer> rendererClass) {
        this.rendererClass = rendererClass;
        reload();
    }

    public void drawHoveringText(ItemStack stack, int x, int y) {
        drawHoveringText(null, stack, new ArrayList<>(), x, y);
    }

    public void drawHoveringText(List<?> textLines, int x, int y) {
        drawHoveringText(null, null, textLines, x, y);
    }

    public void drawHoveringText(ItemStack stack, List<?> textLines, int x, int y) {
        drawHoveringText(null, stack, textLines, x, y);
    }

    public void drawHoveringText(String context, ItemStack stack, List<?> textLines, int x, int y) {
        final int currentHash = ClientUtil.getMetaHash();
        boolean updateContent = false;

        if (stack == null && textLines.isEmpty()) {
            return;
        }

        if (this.lastContext != null
            && (Math.abs(this.lastContext.getMouseX() - x) > 16 || Math.abs(this.lastContext.getMouseY() - y) > 16)) {
            clearCache();
        }

        if (!updateContent && this.lastContext != null
            && !ItemStack.areItemStacksEqual(lastContext.getStack(), stack)) {
            clearCache();
        }

        if (!updateContent && this.lastTextLines != null && !equalsTextLines(this.lastTextLines, textLines)) {
            if (this.lastContext != null && this.lastContext.getStack() == null
                && this.lastContext.getMouseX() == x
                && this.lastContext.getMouseY() == y) {
                updateContent = true;
            } else {
                clearCache();
            }
        }

        if (this.lastContext == null) {
            this.lastContext = new TooltipContext(context, getRendererFor(context, stack), stack);
            this.lastContext.setPosition(x, y);

            this.lastHashMode = currentHash;
            this.lastTextLines = new ArrayList<>(textLines);

            enrichTooltip(this.lastContext, TooltipHandler.convertList(textLines));
        } else if (updateContent || this.lastHashMode != currentHash) {
            this.lastContext.clearComponents();
            this.lastContext.setPosition(x, y);

            this.lastHashMode = currentHash;
            this.lastTextLines = new ArrayList<>(textLines);

            enrichTooltip(this.lastContext, TooltipHandler.convertList(textLines));
        } else {
            this.lastContext.setPosition(x, y);
        }

        this.ignoreLastTooltip = false;
    }

    protected void clearCache() {
        this.lastContext = null;
        this.ignoreLastTooltip = true;
        this.lastTextLines = null;
        this.lastHashMode = 0;
    }

    public void enrichTooltip(TooltipContext context, List<ITooltipComponent> lines) {

        if (context.getStack() == null && !lines.isEmpty() && lines.get(0) instanceof TextTooltipComponent) {
            context.addSectionComponent("title", Arrays.asList(lines.remove(0)));
        }

        for (Map.Entry<String, ITooltipEnricher> entry : this.tooltipEnrichers.entrySet()) {
            context.addSectionComponent(
                entry.getKey(),
                entry.getValue()
                    .enrich(context));
        }

        if (!lines.isEmpty()) {
            context.addSectionComponent(
                context.getComponents()
                    .size() - 2,
                "tooltip",
                lines);
        }

        ClientUtil.postEvent(new TooltipEnricherEvent(context));
    }

    public ITooltipRenderer getRendererFor(String context, ItemStack stack) {

        if (context == null) {
            context = stack != null ? "item" : "default";
        }

        for (ITooltipRenderer renderer : this.otherTooltipRenderers.getOrDefault(context, Collections.emptyList())) {
            if (renderer.matches(stack)) {
                return renderer;
            }
        }

        if (stack != null && !"item".equals(context)) {

            for (ITooltipRenderer renderer : this.otherTooltipRenderers.getOrDefault("item", Collections.emptyList())) {
                if (renderer.matches(stack)) {
                    return renderer;
                }
            }

        }

        return this.defaultTooltipRenderer;
    }

    protected boolean equalsTextLines(List<?> a, List<?> b) {
        if (a.size() != b.size()) return false;

        for (int i = 0; i < a.size(); i++) {
            if (!Objects.equals(a.get(i), b.get(i))) return false;
        }

        return true;
    }

    public Rectangle getLastTooltipBounds() {

        if (this.lastContext != null) {
            return this.lastContext.getRenderer()
                .getTooltipBounds(this.lastContext);
        }

        return null;
    }

    public void drawLastTooltip() {

        if (this.lastContext != null && !this.lastContext.isEmpty() && !this.ignoreLastTooltip) {
            this.lastContext.getRenderer()
                .draw(this.lastContext);
            this.ignoreLastTooltip = true;
        } else if (this.lastContext != null && this.ignoreLastTooltip) {
            clearCache();
        }

    }

    public boolean nextTooltipPage() {

        if (this.lastContext != null) {
            return this.lastContext.getRenderer()
                .nextTooltipPage();
        }

        return false;
    }

    public boolean previousTooltipPage() {

        if (this.lastContext != null) {
            return this.lastContext.getRenderer()
                .previousTooltipPage();
        }

        return false;
    }

    public static List<ITooltipComponent> convertList(List<?> list) {
        final List<ITooltipComponent> results = new ArrayList<>();
        final TextLinesConverterEvent event = new TextLinesConverterEvent(instance.lastContext, list);
        ClientUtil.postEvent(event);

        for (Object line : event.list) {

            if (line instanceof ITooltipComponent component) {
                results.add(component);
            } else if ("".equals(line)) {
                results.add(
                    instance.lastContext.getRenderer()
                        .getParagraphSpacing());
            } else if (line instanceof String str) {
                ITooltipComponent component = TooltipHandler.getTooltipComponent(str);

                if (component == null) {
                    component = new TextTooltipComponent(str);
                }

                results.add(component);
            }

        }

        return results;
    }

}
