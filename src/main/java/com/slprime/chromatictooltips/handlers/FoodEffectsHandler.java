package com.slprime.chromatictooltips.handlers;

import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import com.slprime.chromatictooltips.event.FoodEffectsEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class FoodEffectsHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFoodEffects(FoodEffectsEvent event) {
        final ItemStack stack = event.target.getItem();

        if (stack.getItem() instanceof ItemFood food) {

            if (food instanceof ItemAppleGold) {
                event.addPotionEffect(new PotionEffect(Potion.field_76444_x.id, 2400, 0));

                if (stack.getItemDamage() > 0) {
                    event.addPotionEffect(new PotionEffect(Potion.regeneration.id, 600, 4));
                    event.addPotionEffect(new PotionEffect(Potion.resistance.id, 6000, 0));
                    event.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 6000, 0));
                } else {
                    addDefaultFoodEffects(food, event);
                }

            } else if (food instanceof ItemFishFood) {
                final ItemFishFood.FishType fishtype = ItemFishFood.FishType.func_150978_a(stack);

                if (fishtype == ItemFishFood.FishType.PUFFERFISH) {
                    event.addPotionEffect(new PotionEffect(Potion.poison.id, 1200, 3));
                    event.addPotionEffect(new PotionEffect(Potion.hunger.id, 300, 2));
                    event.addPotionEffect(new PotionEffect(Potion.confusion.id, 300, 1));
                }

                addDefaultFoodEffects(food, event);
            } else {
                addDefaultFoodEffects(food, event);
            }
        }

    }

    protected void addDefaultFoodEffects(ItemFood food, FoodEffectsEvent event) {
        if (food.potionId > 0 && food.potionEffectProbability > 0.0F) {
            event.addPotionEffect(
                new PotionEffect(food.potionId, food.potionDuration * 20, food.potionAmplifier),
                food.potionEffectProbability);
        }
    }

}
