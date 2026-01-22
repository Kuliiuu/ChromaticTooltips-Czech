package com.slprime.chromatictooltips;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import net.minecraft.client.resources.IResource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.slprime.chromatictooltips.api.EnricherPlace;
import com.slprime.chromatictooltips.api.ITooltipComponent;
import com.slprime.chromatictooltips.api.ITooltipEnricher;
import com.slprime.chromatictooltips.api.ITooltipRenderer;
import com.slprime.chromatictooltips.api.ITooltipRequestResolver;
import com.slprime.chromatictooltips.api.TooltipBuilder;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.api.TooltipLines;
import com.slprime.chromatictooltips.api.TooltipModifier;
import com.slprime.chromatictooltips.api.TooltipRequest;
import com.slprime.chromatictooltips.api.TooltipStyle;
import com.slprime.chromatictooltips.component.SectionComponent;
import com.slprime.chromatictooltips.config.EnricherConfig;
import com.slprime.chromatictooltips.event.TooltipEnricherEvent;
import com.slprime.chromatictooltips.util.Parser;
import com.slprime.chromatictooltips.util.TooltipUtils;

public class TooltipHandler {

    private static class TooltipCache {

        public long lastFrame = -1;
        public long lastUpdateTime = -1;
        public TooltipContext context = null;
        public TooltipRequest request = null;
        public int hashCode = -1;

        public boolean isEmpty() {
            return this.context == null;
        }

        public void clear() {
            this.lastFrame = -1;
            this.lastUpdateTime = -1;
            this.context = null;
            this.request = null;
            this.hashCode = -1;
        }
    }

    protected static final WeakHashMap<ITooltipComponent, String> tipLineComponents = new WeakHashMap<>();
    protected static int nextComponentId = 0;

    protected static final String CONFIG_FILE = "tooltip.json";
    protected static final String COMPONENT_PREFIX = "\u00A7z";

    protected static ITooltipRenderer defaultTooltipRenderer = null;
    protected static Map<String, List<ITooltipRenderer>> otherTooltipRenderers = new HashMap<>();

    protected static final Parser parser = new Parser();
    protected static Class<? extends ITooltipRenderer> rendererClass = null;
    protected static final List<ITooltipEnricher> tooltipEnrichers = new ArrayList<>();
    protected static final List<ITooltipRequestResolver> requestResolvers = new ArrayList<>();

    protected static final TooltipCache tooltipCache = new TooltipCache();
    protected static Point lastMousePosition = null;
    protected static boolean ignoreLastTooltip = true;

    public static void reload() {
        TooltipHandler.otherTooltipRenderers.clear();
        TooltipHandler.defaultTooltipRenderer = null;

        loadTooltipResource();

        if (TooltipHandler.defaultTooltipRenderer == null) {
            TooltipHandler.defaultTooltipRenderer = createRenderer(new TooltipStyle());
        }
    }

    protected static void parseStyle(String json) {
        final List<TooltipStyle> scopes = TooltipHandler.parser.parse(json);

        for (TooltipStyle style : scopes) {
            String context = style.getAsString("context", null);

            if (context == null && style.containsKey("filter")) {
                context = "item";
            }

            if (context == null || "default".equals(context)) {
                TooltipHandler.defaultTooltipRenderer = createRenderer(style);
            } else {
                TooltipHandler.otherTooltipRenderers.computeIfAbsent(context, k -> new ArrayList<>())
                    .add(createRenderer(style));
            }

        }

    }

    protected static void loadTooltipResource() {
        final ResourceLocation location = new ResourceLocation(ChromaticTooltips.MODID, CONFIG_FILE);

        try {
            final IResource res = TooltipUtils.mc()
                .getResourceManager()
                .getResource(location);

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                ChromaticTooltips.LOG.info("Loading '{}' from resourcepack {}", CONFIG_FILE, location);
                parseStyle(
                    reader.lines()
                        .collect(Collectors.joining("\n")));
            }

        } catch (Exception io) {
            ChromaticTooltips.LOG.error("Failed to load '{}' resourcepack {}", CONFIG_FILE, location);
            io.printStackTrace();
        }

    }

    protected static ITooltipRenderer createRenderer(TooltipStyle style) {
        try {
            return TooltipHandler.rendererClass.getConstructor(TooltipStyle.class)
                .newInstance(style);
        } catch (Exception e1) {
            return new TooltipRenderer(style);
        }
    }

    public static String getComponentId(ITooltipComponent component) {
        return TooltipHandler.tipLineComponents
            .computeIfAbsent(component, k -> TooltipHandler.COMPONENT_PREFIX + (TooltipHandler.nextComponentId++));
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

    public static void addEnricher(ITooltipEnricher enricher) {
        TooltipHandler.tooltipEnrichers.add(enricher);
    }

    public static void addRequestResolver(ITooltipRequestResolver resolver) {
        TooltipHandler.requestResolvers.add(resolver);
    }

    public static void addEnricherAfter(String sectionId, ITooltipEnricher enricher) {
        int index = -1;

        for (int i = 0; i < TooltipHandler.tooltipEnrichers.size(); i++) {
            if (TooltipHandler.tooltipEnrichers.get(i)
                .sectionId()
                .equalsIgnoreCase(sectionId)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            TooltipHandler.tooltipEnrichers.add(index + 1, enricher);
        } else {
            TooltipHandler.tooltipEnrichers.add(enricher);
        }
    }

    public static void setRendererClass(Class<? extends ITooltipRenderer> rendererClass) {
        TooltipHandler.rendererClass = rendererClass;
        reload();
    }

    public static TooltipBuilder builder() {
        return new TooltipBuilder();
    }

    public static void drawHoveringText(ItemStack stack, List<?> textLines) {
        drawHoveringText(new TooltipRequest(null, stack, null, new TooltipLines(textLines), null));
    }

    public static void drawHoveringText(List<?> textLines) {
        drawHoveringText(new TooltipRequest(null, null, null, new TooltipLines(textLines), null));
    }

    public static void drawHoveringText(TooltipRequest request) {

        if (request == null || request.itemStack == null && request.fluidStack == null && request.tooltip.isEmpty()) {
            return;
        }

        // maximize performance by limiting updates (60 FPS target)
        if ((System.currentTimeMillis() - TooltipHandler.tooltipCache.lastFrame) > 16) {
            final int currentHash = TooltipUtils.getMetaHash();

            if (TooltipHandler.tooltipCache.isEmpty() || !request.sameSubjectAs(TooltipHandler.tooltipCache.request)) {
                TooltipHandler.tooltipCache.request = request.copy();
                TooltipHandler.tooltipCache.context = createTooltipContext(request, null);
                TooltipHandler.tooltipCache.lastUpdateTime = System.currentTimeMillis();
            } else if (TooltipHandler.tooltipCache.hashCode != currentHash
                || (System.currentTimeMillis() - TooltipHandler.tooltipCache.lastUpdateTime) > 100
                || !request.equivalentTo(TooltipHandler.tooltipCache.request)) {
                    TooltipHandler.tooltipCache.request = request.copy();
                    TooltipHandler.tooltipCache.context = createTooltipContext(
                        request,
                        TooltipHandler.tooltipCache.context);
                    TooltipHandler.tooltipCache.lastUpdateTime = System.currentTimeMillis();
                }

            TooltipHandler.tooltipCache.hashCode = currentHash;
            TooltipHandler.tooltipCache.lastFrame = System.currentTimeMillis();
        }

        TooltipHandler.lastMousePosition = request.mouse != null ? request.mouse : TooltipUtils.getMousePosition();
        TooltipHandler.ignoreLastTooltip = false;
    }

    public static TooltipContext createTooltipContext(TooltipRequest request, TooltipContext previousContext) {
        TooltipHandler.resolveRequest(request);
        TooltipContext context;

        if (previousContext != null) {
            context = new TooltipContext(request, previousContext);
        } else {
            context = new TooltipContext(request, getRendererFor(request));
        }

        if (EnricherConfig.keyboardModifiersEnabled) {
            updateSupportedModifiers(context);
        }

        enrichTooltip(context);
        return context;
    }

    protected static void resolveRequest(TooltipRequest request) {
        for (ITooltipRequestResolver resolver : TooltipHandler.requestResolvers) {
            if (resolver.resolve(request)) {
                break;
            }
        }
    }

    protected static void enrichTooltip(TooltipContext context) {
        final TooltipModifier activeModifier = TooltipUtils.getActiveModifier();
        final TooltipStyle style = context.getRenderer()
            .getStyle();
        final ITooltipRenderer renderer = context.getRenderer();
        final List<SectionComponent> headerSections = new ArrayList<>();
        final List<SectionComponent> bodySections = new ArrayList<>();
        final List<SectionComponent> footerSections = new ArrayList<>();
        final Comparator<SectionComponent> byOrder = Comparator
            .comparingInt(s -> style.getAsInt("sections." + s.getSectionId() + ".order", 0));

        context.setActiveModifier(activeModifier);

        for (ITooltipEnricher enricher : TooltipHandler.tooltipEnrichers) {
            final EnumSet<TooltipModifier> modes = renderer.getEnricherModes(enricher.sectionId(), enricher.mode());
            final EnricherPlace place = renderer.getEnricherPlace(enricher.sectionId(), enricher.place());

            if (modes.contains(TooltipModifier.NONE) && (place == EnricherPlace.HEADER || place == EnricherPlace.FOOTER)
                || modes.contains(activeModifier)) {
                final TooltipLines result = enricher.build(context);

                if (result != null && !result.isEmpty()) {
                    final String sectionId = enricher.sectionId();
                    final SectionComponent section = new SectionComponent(
                        sectionId,
                        renderer.getSectionBox("sections." + sectionId),
                        result.buildComponents(context));

                    if (place == EnricherPlace.HEADER) {
                        headerSections.add(section);
                    } else if (place == EnricherPlace.BODY) {
                        bodySections.add(section);
                    } else if (place == EnricherPlace.FOOTER) {
                        footerSections.add(section);
                    }

                }
            }
        }

        if (bodySections.isEmpty() && activeModifier != TooltipModifier.NONE) {
            bodySections.addAll(fallbackBuildBodyList(context));
        }

        headerSections.sort(byOrder);
        bodySections.sort(byOrder);
        footerSections.sort(byOrder);

        context.addSection("header", new ArrayList<>(headerSections));
        context.addSection("body", new ArrayList<>(bodySections));
        context.addSection("footer", new ArrayList<>(footerSections));

        TooltipUtils.postEvent(new TooltipEnricherEvent(context));
    }

    public static void updateSupportedModifiers(TooltipContext context) {
        final ITooltipRenderer renderer = context.getRenderer();

        for (ITooltipEnricher enricher : TooltipHandler.tooltipEnrichers) {
            final EnricherPlace place = renderer.getEnricherPlace(enricher.sectionId(), enricher.place());

            if ("itemInfo".equals(enricher.sectionId()) || place != EnricherPlace.BODY) {
                continue;
            }

            final EnumSet<TooltipModifier> modes = renderer.getEnricherModes(enricher.sectionId(), enricher.mode());
            TooltipLines noneComponents = null;

            if (modes.contains(TooltipModifier.NONE)) {
                context.setActiveModifier(TooltipModifier.NONE);
                noneComponents = enricher.build(context);
            }

            for (TooltipModifier modifier : modes) {

                if (modifier == TooltipModifier.NONE || context.getSupportedModifiers()
                    .contains(modifier)) {
                    continue;
                }

                context.setActiveModifier(modifier);
                final TooltipLines result = enricher.build(context);

                if (result != null && !result.isEmpty() && (noneComponents == null || !result.equals(noneComponents))) {
                    context.supportModifiers(modifier);
                }
            }

        }
    }

    protected static List<SectionComponent> fallbackBuildBodyList(TooltipContext context) {
        final List<SectionComponent> bodySections = new ArrayList<>();
        final ITooltipRenderer renderer = context.getRenderer();
        context.setActiveModifier(TooltipModifier.NONE);

        for (ITooltipEnricher enricher : TooltipHandler.tooltipEnrichers) {
            final EnumSet<TooltipModifier> modes = renderer.getEnricherModes(enricher.sectionId(), enricher.mode());
            final EnricherPlace place = renderer.getEnricherPlace(enricher.sectionId(), enricher.place());

            if (place == EnricherPlace.BODY && modes.contains(TooltipModifier.NONE)) {
                final TooltipLines result = enricher.build(context);

                if (result != null && !result.isEmpty()) {
                    final String sectionId = enricher.sectionId();
                    final SectionComponent section = new SectionComponent(
                        sectionId,
                        renderer.getSectionBox("sections." + sectionId),
                        result.buildComponents(context));
                    bodySections.add(section);
                }
            }
        }

        return bodySections;
    }

    protected static ITooltipRenderer getRendererFor(TooltipRequest request) {
        final String fallbackContext = request.itemStack != null ? "item"
            : (request.fluidStack != null ? "fluid" : "default");
        final String context = request.context != null ? request.context : fallbackContext;

        if ("default".equals(context)) {
            return TooltipHandler.defaultTooltipRenderer;
        }

        ITooltipRenderer renderer = findRenderer(context, request);

        if (renderer == null && (request.itemStack != null || request.fluidStack != null)
            && !fallbackContext.equals(context)) {
            renderer = findRenderer(fallbackContext, request);
        }

        if (renderer == null && request.fluidStack != null) {
            renderer = findRenderer("item", null);
        }

        return renderer != null ? renderer : TooltipHandler.defaultTooltipRenderer;
    }

    private static ITooltipRenderer findRenderer(String context, TooltipRequest request) {
        for (ITooltipRenderer renderer : TooltipHandler.otherTooltipRenderers
            .getOrDefault(context, Collections.emptyList())) {
            if (renderer.matches(request)) {
                return renderer;
            }
        }
        return null;
    }

    public static TooltipContext getLastTooltipContext() {
        return TooltipHandler.tooltipCache.context;
    }

    public static void drawLastTooltip() {

        if (!TooltipHandler.tooltipCache.isEmpty() && !TooltipHandler.tooltipCache.context.isEmpty()
            && !TooltipHandler.ignoreLastTooltip) {
            TooltipHandler.tooltipCache.context
                .drawAtMousePosition(TooltipHandler.lastMousePosition.x, TooltipHandler.lastMousePosition.y);
            TooltipHandler.ignoreLastTooltip = true;
        } else if (!TooltipHandler.tooltipCache.isEmpty() && TooltipHandler.ignoreLastTooltip) {
            TooltipHandler.tooltipCache.clear();
        }

    }

    public static boolean nextTooltipPage() {

        if (!TooltipHandler.tooltipCache.isEmpty()) {
            return TooltipHandler.tooltipCache.context.nextTooltipPage();
        }

        return false;
    }

    public static boolean previousTooltipPage() {

        if (!TooltipHandler.tooltipCache.isEmpty()) {
            return TooltipHandler.tooltipCache.context.previousTooltipPage();
        }

        return false;
    }

}
