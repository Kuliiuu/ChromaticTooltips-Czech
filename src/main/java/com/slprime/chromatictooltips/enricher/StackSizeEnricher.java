package com.slprime.chromatictooltips.enricher;

import java.awt.Point;
import java.util.EnumSet;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.slprime.chromatictooltips.api.EnricherPlace;
import com.slprime.chromatictooltips.api.ITooltipEnricher;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.api.TooltipLines;
import com.slprime.chromatictooltips.api.TooltipModifier;
import com.slprime.chromatictooltips.config.EnricherConfig;
import com.slprime.chromatictooltips.event.StackSizeEnricherEvent;
import com.slprime.chromatictooltips.util.TooltipUtils;

public class StackSizeEnricher implements ITooltipEnricher {

    @Override
    public String sectionId() {
        return "stacksize";
    }

    @Override
    public EnricherPlace place() {
        return EnricherPlace.BODY;
    }

    @Override
    public EnumSet<TooltipModifier> mode() {
        return EnumSet.of(TooltipModifier.SHIFT);
    }

    @Override
    public TooltipLines build(TooltipContext context) {

        if (!EnricherConfig.stackSizeEnabled) {
            return null;
        }

        final ItemStack stack = context.getItemStack();
        final FluidStack fluid = context.getFluidStack();
        Fluid containedFluid = null;
        long containedFluidAmount = 0;
        long stackAmount = 0;

        if (stack != null) {
            final FluidStack stackFluid = getFluid(stack);
            stackAmount = EnricherConfig.includeContainerInventoryEnabled ? getStackSize(stack) : stack.stackSize;

            if (stackFluid != null) {
                containedFluid = stackFluid.getFluid();
                containedFluidAmount = stackFluid.amount;
            }

        } else if (fluid != null) {
            stackAmount = fluid.amount;
        } else {
            return null;
        }

        final StackSizeEnricherEvent event = new StackSizeEnricherEvent(
            context,
            stackAmount,
            containedFluid,
            containedFluidAmount);
        TooltipUtils.postEvent(event);

        if (event.stackAmount <= 0) {
            return null;
        }

        final TooltipLines components = new TooltipLines();

        if (stack != null && event.stackAmount > 1
            && (!EnricherConfig.showOnlyWhenOverStackSizeEnabled || event.stackAmount != stack.stackSize
                || event.stackAmount >= 10_000)) {
            components.line(formatStackSize(event.stackAmount, stack.getMaxStackSize()));
        }

        if (stack != null && event.containedFluid != null && event.containedFluidAmount > 0) {
            components.line(formatFluidAmount(event.stackAmount * event.containedFluidAmount));
        }

        if (fluid != null && event.stackAmount > 0
            && (!EnricherConfig.showOnlyWhenOverStackSizeEnabled || event.stackAmount >= 10_000)) {
            components.line(formatFluidAmount(event.stackAmount));
        }

        return components;
    }

    protected long getStackSize(ItemStack stack) {
        final GuiContainer guiContainer = TooltipUtils.getGuiContainer();

        if (guiContainer != null) {
            final Point mouse = TooltipUtils.getMousePosition();
            final Slot slot = guiContainer.getSlotAtPosition(mouse.x, mouse.y);

            if (slot != null && slot.getHasStack()) {
                long stackSize = 0;

                for (Slot currentSlot : guiContainer.inventorySlots.inventorySlots) {
                    if (slot.inventory == currentSlot.inventory && currentSlot.getHasStack()
                        && stack.isItemEqual(currentSlot.getStack())
                        && ItemStack.areItemStackTagsEqual(stack, currentSlot.getStack())) {
                        stackSize += currentSlot.getStack().stackSize;
                    }
                }

                return stackSize == 0 ? stack.stackSize : stackSize;
            }
        }

        return stack.stackSize;
    }

    protected FluidStack getFluid(ItemStack stack) {
        FluidStack fluidStack = FluidContainerRegistry.getFluidForFilledItem(stack);

        if (fluidStack == null && stack.getItem() instanceof IFluidContainerItem fluidItem) {
            fluidStack = fluidItem.getFluid(stack);
        }

        return fluidStack;
    }

    protected String formatStackSize(long stackAmount, int maxStackSize) {
        return format(
            stackAmount,
            maxStackSize,
            "enricher.stacksize.item.full",
            "enricher.stacksize.item.short",
            "enricher.stacksize.item");
    }

    protected String formatFluidAmount(long stackAmount) {
        return format(
            stackAmount,
            144,
            "enricher.stacksize.fluid.full",
            "enricher.stacksize.fluid.short",
            "enricher.stacksize.fluid");
    }

    protected String format(long stackAmount, int maxStackSize, String fullPattern, String shortPattern,
        String stackPattern) {

        if (stackAmount <= maxStackSize || maxStackSize == 1) {
            return TooltipUtils.translate(stackPattern, TooltipUtils.formatNumbers(stackAmount));
        }

        final int remainder = (int) (stackAmount % maxStackSize);

        if (remainder > 0) {
            return TooltipUtils.translate(
                fullPattern,
                TooltipUtils.formatNumbers(stackAmount),
                TooltipUtils.formatNumbers(stackAmount / maxStackSize),
                TooltipUtils.formatNumbers(maxStackSize),
                TooltipUtils.formatNumbers(remainder));
        } else {
            return TooltipUtils.translate(
                shortPattern,
                TooltipUtils.formatNumbers(stackAmount),
                TooltipUtils.formatNumbers(stackAmount / maxStackSize),
                TooltipUtils.formatNumbers(maxStackSize));
        }
    }

}
