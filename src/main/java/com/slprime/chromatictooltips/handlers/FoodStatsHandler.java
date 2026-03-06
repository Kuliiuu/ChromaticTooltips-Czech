package com.slprime.chromatictooltips.handlers;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;

import com.slprime.chromatictooltips.event.FoodStatsEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class FoodStatsHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFoodStats(FoodStatsEvent event) {
        final ItemStack stack = event.target.getItem();

        if (stack.getItem() instanceof ItemFood food) {
            event.hunger = food.func_150905_g(stack);
            event.saturationModifier = food.func_150906_h(stack);
        }
    }

}
