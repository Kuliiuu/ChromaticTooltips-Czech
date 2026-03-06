package com.slprime.chromatictooltips.event;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.potion.PotionEffect;

import com.slprime.chromatictooltips.api.TooltipTarget;

public class FoodEffectsEvent extends TooltipEvent {

    public Map<PotionEffect, Float> effects = new HashMap<>();

    public FoodEffectsEvent(TooltipTarget target) {
        super(target);
    }

    public void addPotionEffect(PotionEffect effect, float probability) {
        this.effects.put(effect, probability);
    }

    public void addPotionEffect(PotionEffect effect) {
        this.effects.put(effect, 1.0F);
    }

}
