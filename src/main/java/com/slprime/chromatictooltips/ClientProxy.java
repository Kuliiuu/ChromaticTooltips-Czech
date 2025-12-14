package com.slprime.chromatictooltips;

import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.slprime.chromatictooltips.enricher.HotkeyEnricher;
import com.slprime.chromatictooltips.enricher.ItemInfoEnricher;
import com.slprime.chromatictooltips.enricher.ItemTitleEnricher;
import com.slprime.chromatictooltips.enricher.ModInfoEnricher;
import com.slprime.chromatictooltips.enricher.StackSizeEnricher;
import com.slprime.chromatictooltips.util.ClientUtil;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ClientProxy extends CommonProxy {

    public static final KeyBinding nextPage = new KeyBinding(
        "key.chromatictooltips.next_page",
        Keyboard.KEY_Z,
        "key.categories.chromatictooltips");
    public static final KeyBinding previousPage = new KeyBinding(
        "key.chromatictooltips.previous_page",
        Keyboard.KEY_NONE,
        "key.categories.chromatictooltips");
    protected static boolean nextPageIsPressed = false;
    protected static boolean previousPageIsPressed = false;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);

        ClientRegistry.registerKeyBinding(nextPage);
        ClientRegistry.registerKeyBinding(previousPage);

        TooltipHandler.instance()
            .addEnricher("itemTitle", new ItemTitleEnricher());
        TooltipHandler.instance()
            .addEnricher("stackSize", new StackSizeEnricher());
        TooltipHandler.instance()
            .addEnricher("hotkeys", new HotkeyEnricher());
        TooltipHandler.instance()
            .addEnricher("itemInfo", new ItemInfoEnricher());
        TooltipHandler.instance()
            .addEnricher("modInfo", new ModInfoEnricher());

        TooltipHandler.instance()
            .setRendererClass(TooltipRenderer.class);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);

        if (ClientUtil.mc()
            .getResourceManager() instanceof SimpleReloadableResourceManager manager) {
            manager.registerReloadListener(
                resourceManager -> TooltipHandler.instance()
                    .reload());
        }
    }

    @SubscribeEvent
    public void onScreenPostDraw(DrawScreenEvent.Post event) {
        TooltipHandler.instance()
            .drawLastTooltip();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;

        final boolean nextPressed = nextPage.getKeyCode() != 0 && Keyboard.isKeyDown(nextPage.getKeyCode());
        final boolean previousPressed = previousPage.getKeyCode() != 0 && Keyboard.isKeyDown(previousPage.getKeyCode());

        if (nextPressed && !nextPageIsPressed) {
            TooltipHandler.instance()
                .nextTooltipPage();
        } else if (previousPressed && !previousPageIsPressed) {
            TooltipHandler.instance()
                .previousTooltipPage();
        }

        nextPageIsPressed = nextPressed;
        previousPageIsPressed = previousPressed;
    }

}
