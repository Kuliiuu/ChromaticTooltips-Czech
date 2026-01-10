package com.slprime.chromatictooltips.api;

import java.util.UUID;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.slprime.chromatictooltips.component.ItemAttributeComponent;
import com.slprime.chromatictooltips.component.TextComponent;
import com.slprime.chromatictooltips.util.ClientUtil;

public class ItemStats {

    public static class ArmorStats extends ItemStats {

        public ArmorStats(int damageReduceAmount) {
            super(
                ClientUtil.translate("enricher.attributes.armor.text", ClientUtil.formatNumbers(damageReduceAmount)),
                ClientUtil.translate("enricher.attributes.armor.icon", ClientUtil.formatNumbers(damageReduceAmount)),
                damageReduceAmount,
                "attributes/armor.png");
        }

    }

    public static class AttackDamageStats extends ItemStats {

        public AttackDamageStats(double attackDamage) {
            super(
                StatCollector.translateToLocal("attribute.name.generic.attackDamage"),
                attackDamage,
                StatsOperator.ADDITION,
                "attributes/attack_damage.png");
        }

    }

    public static class MaxHealthStats extends ItemStats {

        public MaxHealthStats(double maxHealth) {
            super(
                StatCollector.translateToLocal("attribute.name.generic.maxHealth"),
                maxHealth,
                StatsOperator.ADDITION,
                "attributes/max_health.png");
        }

    }

    public static class KnockbackResistanceStats extends ItemStats {

        public KnockbackResistanceStats(double knockbackResistance) {
            super(
                StatCollector.translateToLocal("attribute.name.generic.knockbackResistance"),
                knockbackResistance,
                StatsOperator.ADDITION,
                "attributes/knockback_resistance.png");
        }

    }

    public static class MovementSpeedStats extends ItemStats {

        public MovementSpeedStats(double movementSpeed) {
            super(
                StatCollector.translateToLocal("attribute.name.generic.movementSpeed"),
                movementSpeed,
                StatsOperator.ADDITION,
                "attributes/movement_speed.png");
        }

    }

    public static class UnbreakableStats extends ItemStats {

        public UnbreakableStats() {
            super(StatCollector.translateToLocal("item.unbreakable"), null, 0, null);
        }

        @Override
        public int getOrder() {
            return -10_000;
        }

    }

    public static class DurabilityStats extends ItemStats {

        public DurabilityStats(int durability, int maxDurability) {
            super(
                ClientUtil.translate(
                    "enricher.attributes.durability.text",
                    ClientUtil.formatNumbers(durability),
                    ClientUtil.formatNumbers(maxDurability)),
                ClientUtil.translate(
                    "enricher.attributes.durability.icon",
                    ClientUtil.formatNumbers(durability),
                    ClientUtil.formatNumbers(maxDurability)),
                durability,
                "attributes/durability.png");
        }

        @Override
        public int getOrder() {
            return -10_010;
        }

    }

    public static class BurnTimeStats extends ItemStats {

        public BurnTimeStats(int burnTime) {
            super(
                ClientUtil.translate("enricher.attributes.fuel.text", ClientUtil.formatNumbers(burnTime)),
                ClientUtil.translate("enricher.attributes.fuel.icon", ClientUtil.formatNumbers(burnTime)),
                burnTime,
                "attributes/fuel.png");
        }

    }

    public static enum StatsOperator {

        ADDITION(0, "addition"), // +5 Damage
        MULTIPLY_BASE(1, "percent"), // +10% Damage
        MULTIPLY_TOTAL(2, "percent"); // +10% Damage after all modifiers

        private final int operation;
        private final String key;

        StatsOperator(int operation, String key) {
            this.operation = operation;
            this.key = key;
        }

        public int getOperation() {
            return this.operation;
        }

        public String getKey() {
            return this.key;
        }

        public static StatsOperator fromOperation(int operation) {
            for (StatsOperator op : values()) {
                if (op.getOperation() == operation) {
                    return op;
                }
            }
            return null;
        }

    }

    // copied from Item
    protected static final UUID ITEM_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");

    protected String textLine;
    protected String iconLine;
    protected double value;
    protected String icon;

    public ItemStats(String textLine, String iconLine, double value, String icon) {
        this.textLine = textLine;
        this.iconLine = iconLine;
        this.value = value;
        this.icon = icon;
    }

    public ItemStats(String attributeName, double value, StatsOperator operator, String icon) {
        final String attributeValue = ClientUtil.formatNumbers(Math.abs(value));
        final String dir = value >= 0 ? "plus" : "minus";

        this.textLine = ClientUtil
            .translate("enricher.attributes." + operator.getKey() + ".text." + dir, attributeValue, attributeName);
        this.iconLine = ClientUtil
            .translate("enricher.attributes." + operator.getKey() + ".icon." + dir, attributeValue);
        this.value = value;
        this.icon = icon;
    }

    public ItemStats withIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public boolean hasIcon() {
        return this.icon != null;
    }

    public double getValue() {
        return this.value;
    }

    public int getOrder() {
        return (int) (100 * this.value);
    }

    public static double getModifiedAmount(AttributeModifier modifier, ItemStack stack) {
        double amount = modifier.getAmount();

        if (modifier.getID() == ITEM_UUID) {
            amount += EnchantmentHelper.func_152377_a(stack, EnumCreatureAttribute.UNDEFINED);
        }

        if (modifier.getOperation() != 0) {
            amount *= 100;
        }

        return amount;
    }

    public ITooltipComponent getIconComponent() {

        if (this.icon != null && this.iconLine != null) {
            return new ItemAttributeComponent(this.icon, this.iconLine);
        }

        return null;
    }

    public ITooltipComponent getTextComponent() {
        return this.textLine != null
            ? new TextComponent(ClientUtil.applyBaseColorIfAbsent(this.textLine, TooltipLines.BASE_COLOR))
            : null;
    }

}
