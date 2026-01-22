package com.slprime.chromatictooltips.api;

import java.awt.Point;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

public class TooltipRequest {

    public final String context;
    public ItemStack itemStack;
    public FluidStack fluidStack;
    public final TooltipLines tooltip;
    public final Point mouse;

    public TooltipRequest(String context, ItemStack itemStack, FluidStack fluidStack, TooltipLines tooltip,
        Point mouse) {
        this.context = context;
        this.itemStack = itemStack;
        this.fluidStack = fluidStack;
        this.tooltip = tooltip != null ? tooltip : new TooltipLines();
        this.mouse = mouse;
    }

    public TooltipRequest(TooltipRequest request) {
        this(request.context, request.itemStack, request.fluidStack, new TooltipLines(request.tooltip), request.mouse);
    }

    public TooltipRequest copy() {
        return new TooltipRequest(this);
    }

    public boolean sameSubjectAs(TooltipRequest other) {
        if (other == null || !sameSubjectPresence(other)) return false;

        if (this.fluidStack != null && !this.fluidStack.isFluidEqual(other.fluidStack)) {
            return false;
        }

        if (this.itemStack != null && !areStacksSameType(this.itemStack, other.itemStack)) {
            return false;
        }

        return true;
    }

    public boolean equivalentTo(TooltipRequest other) {
        if (!sameSubjectAs(other)) return false;

        if (fluidStack != null && fluidStack.amount != other.fluidStack.amount) {
            return false;
        }

        if (itemStack != null && !areItemStacksEqual(itemStack, other.itemStack)) {
            return false;
        }

        return tooltip.equals(other.tooltip);
    }

    private boolean sameSubjectPresence(TooltipRequest other) {
        return (this.itemStack == null) == (other.itemStack == null)
            && (this.fluidStack == null) == (other.fluidStack == null);
    }

    protected static boolean areItemStacksEqual(ItemStack stackA, ItemStack stackB) {
        if (stackA == stackB) return true;
        if (stackA == null || stackB == null) return false;
        if (stackA.stackSize != stackB.stackSize || !stackA.isItemEqual(stackB)) return false;

        if (stackA.hasTagCompound() && stackB.hasTagCompound()) {
            return stackA.stackTagCompound.equals(stackB.stackTagCompound);
        }

        return (stackA.stackTagCompound == null || stackA.stackTagCompound.hasNoTags())
            && (stackB.stackTagCompound == null || stackB.stackTagCompound.hasNoTags());
    }

    /**
     * Checks if two stacks represent the same logical item type.
     * Ignores stack size and allows wildcard / damageable matching.
     */
    protected static boolean areStacksSameType(ItemStack stackA, ItemStack stackB) {
        if (stackA == stackB) return true;
        if (stackA == null || stackB == null) return false;

        return stackA.getItem() == stackB.getItem() && (stackA.getItemDamage() == stackB.getItemDamage()
            || stackA.getItemDamage() == OreDictionary.WILDCARD_VALUE
            || stackB.getItemDamage() == OreDictionary.WILDCARD_VALUE
            || stackA.getItem()
                .isDamageable());
    }

}
