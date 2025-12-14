package com.slprime.chromatictooltips.enricher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.util.EnumChatFormatting;

import com.slprime.chromatictooltips.Config;
import com.slprime.chromatictooltips.api.ITooltipComponent;
import com.slprime.chromatictooltips.api.ITooltipEnricher;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.component.TextTooltipComponent;
import com.slprime.chromatictooltips.event.HotkeyEnricherEvent;
import com.slprime.chromatictooltips.util.ClientUtil;

public class HotkeyEnricher implements ITooltipEnricher {

    protected String moreText;

    public HotkeyEnricher() {
        this.moreText = EnumChatFormatting.GRAY + ClientUtil.translate(
            "enricher.hotkeys.message",
            EnumChatFormatting.GOLD + ClientUtil.translate("key.alt") + EnumChatFormatting.GRAY);
    }

    @Override
    public List<ITooltipComponent> enrich(TooltipContext context) {

        if (!Config.hotkeysEnricherEnabled) {
            return null;
        }

        ITooltipComponent component = null;

        if (ClientUtil.altKey()) {
            component = hotkeysListComponent(context);
        } else if (Config.hotkeysHelpTextEnabled) {
            component = getHotkeysHelpText(context);
        }

        if (component != null) {
            return Arrays.asList(component);
        }

        return null;
    }

    protected TextTooltipComponent getHotkeysHelpText(TooltipContext context) {
        final HotkeyEnricherEvent event = new HotkeyEnricherEvent(context, new HashMap<>());
        ClientUtil.postEvent(event);

        event.hotkeys.remove(null);
        event.hotkeys.remove("");

        if (!event.hotkeys.isEmpty() && this.moreText != null) {
            return new TextTooltipComponent(this.moreText);
        }

        return null;
    }

    protected TextTooltipComponent hotkeysListComponent(TooltipContext context) {
        final HotkeyEnricherEvent event = new HotkeyEnricherEvent(context, new HashMap<>());
        ClientUtil.postEvent(event);

        event.hotkeys.remove(null);
        event.hotkeys.remove("");

        if (!event.hotkeys.isEmpty()) {
            return new TextTooltipComponent(getHotkeyList(event.hotkeys));
        }

        return null;
    }

    protected List<String> getHotkeyList(Map<String, String> hotkeys) {
        final Map<String, List<String>> messages = new HashMap<>();

        for (Map.Entry<String, String> entry : hotkeys.entrySet()) {
            messages.computeIfAbsent(entry.getValue(), m -> new ArrayList<>())
                .add(entry.getKey());
        }

        for (List<String> keys : messages.values()) {
            Collections.sort(keys, (a, b) -> {
                if (a.length() != b.length()) {
                    return Integer.compare(a.length(), b.length());
                }
                return a.compareTo(b);
            });
        }

        return messages.entrySet()
            .stream()
            .sorted((a, b) -> {
                String sa = String.join("/", a.getValue());
                String sb = String.join("/", b.getValue());

                if (sa.length() != sb.length()) {
                    return Integer.compare(sa.length(), sb.length());
                }

                return sa.compareTo(sb);
            })
            .map(entry -> getHotkeyTip(entry.getValue(), entry.getKey()))
            .collect(Collectors.toList());
    }

    protected String getHotkeyTip(List<String> keys, String message) {
        return EnumChatFormatting.GOLD
            + String.join(EnumChatFormatting.DARK_GRAY + " / " + EnumChatFormatting.GOLD, keys)
            + EnumChatFormatting.DARK_GRAY
            + " - "
            + EnumChatFormatting.GRAY
            + message;
    }

}
