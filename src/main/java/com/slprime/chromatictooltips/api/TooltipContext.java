package com.slprime.chromatictooltips.api;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;

import com.slprime.chromatictooltips.component.SectionComponent;
import com.slprime.chromatictooltips.util.ClientUtil;

public class TooltipContext {

    protected ItemStack stack;
    protected long animationStartTime;
    protected final String context;
    protected List<ITooltipComponent> contextTooltip = new ArrayList<>();
    protected List<SectionComponent> lines = new ArrayList<>();
    protected EnumSet<TooltipModifier> supportedModifiers = EnumSet.noneOf(TooltipModifier.class);
    protected TooltipModifier activeModifier = TooltipModifier.NONE;
    protected int scaleFactor = 1;

    protected final ITooltipRenderer renderer;
    protected int revision = 0;
    protected int mouseX;
    protected int mouseY;

    protected List<SectionComponent> pagedComponents = null;
    protected Dimension tooltipSize;
    protected int lastRevision = 0;
    protected int currentPage = 0;

    public TooltipContext(TooltipRequest request, ITooltipRenderer renderer) {
        this.renderer = renderer;
        this.context = request.context;
        this.animationStartTime = System.currentTimeMillis();
        this.stack = request.stack != null ? request.stack.copy() : null;
        this.contextTooltip = request.tooltip.buildComponents(this);
        this.scaleFactor = ClientUtil.getTooltipScale();
    }

    public void setStack(ItemStack stack) {
        this.stack = stack != null ? stack.copy() : null;
        this.revision++;
    }

    public void setPosition(Point mouse) {
        this.mouseX = mouse.x;
        this.mouseY = mouse.y;
    }

    public String getContextName() {
        return this.context;
    }

    public long getAnimationStartTime() {
        return this.animationStartTime;
    }

    public int getMouseX() {
        return this.mouseX;
    }

    public int getMouseY() {
        return this.mouseY;
    }

    public ITooltipRenderer getRenderer() {
        return this.renderer;
    }

    public TooltipStyle getAsStyle(String path) {
        return this.renderer.getStyle()
            .getAsStyle(path);
    }

    public int getScaleFactor() {
        return this.scaleFactor;
    }

    public void setScaleFactor(int scaleFactor) {
        if (this.scaleFactor != scaleFactor) {
            this.scaleFactor = scaleFactor;
            this.revision++;
        }
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public void supportModifiers(TooltipModifier... modifiers) {
        if (this.supportedModifiers.addAll(Arrays.asList(modifiers))) {
            this.revision++;
        }
    }

    public EnumSet<TooltipModifier> getSupportedModifiers() {
        return supportedModifiers;
    }

    public TooltipModifier getActiveModifier() {
        return this.activeModifier;
    }

    public void setActiveModifier(TooltipModifier activeModifier) {
        if (this.activeModifier != activeModifier) {
            this.activeModifier = activeModifier;
            this.revision++;
        }
    }

    public void setContextTooltip(TooltipLines tooltip) {
        this.contextTooltip = tooltip.buildComponents(this);
        this.revision++;
    }

    public List<ITooltipComponent> getContextTooltip() {
        return Collections.unmodifiableList(this.contextTooltip);
    }

    public List<SectionComponent> getSections() {
        return Collections.unmodifiableList(this.lines);
    }

    public void addSection(int index, String sectionId, List<ITooltipComponent> components) {
        if (components == null || components.isEmpty()) return;
        index = Math.max(0, Math.min(index, this.lines.size()));
        this.lines.add(index, new SectionComponent(sectionId, this.renderer.getSectionBox(sectionId), components));
        this.revision++;
    }

    public void addSection(String sectionId, List<ITooltipComponent> components) {
        if (components == null || components.isEmpty()) return;
        this.lines.add(new SectionComponent(sectionId, this.renderer.getSectionBox(sectionId), components));
        this.revision++;
    }

    public int getRevision() {
        return this.revision;
    }

    public void clear() {
        this.lines.clear();
        this.revision++;
    }

    public boolean isEmpty() {
        return this.lines.isEmpty();
    }

    public SectionComponent getActivePageComponent() {

        if (this.pagedComponents == null || this.lastRevision != this.revision) {
            this.pagedComponents = this.renderer.paginateTooltip(this);
            this.lastRevision = this.revision;
            this.currentPage = Math.max(0, Math.min(this.currentPage, this.pagedComponents.size() - 1));
            this.tooltipSize = null;
        }

        return this.pagedComponents.isEmpty() ? null : this.pagedComponents.get(this.currentPage);
    }

    public boolean nextTooltipPage() {

        if (getActivePageComponent() != null
            && this.currentPage != (this.currentPage = (this.currentPage + 1) % this.pagedComponents.size())) {
            this.tooltipSize = null;
            return true;
        }

        return false;
    }

    public boolean previousTooltipPage() {

        if (getActivePageComponent() != null
            && this.currentPage != (this.currentPage = (this.currentPage - 1 + this.pagedComponents.size())
                % this.pagedComponents.size())) {
            this.tooltipSize = null;
            return true;
        }

        return false;
    }

    public Dimension getTooltipSize() {

        if (this.tooltipSize == null) {
            final SectionComponent section = getActivePageComponent();

            if (section == null) {
                return null;
            }

            final float scaleShift = (float) getScaleFactor() / ClientUtil.getScaledResolution()
                .getScaleFactor();

            this.tooltipSize = new Dimension(
                (int) Math.ceil(section.getWidth() * scaleShift),
                (int) Math.ceil(section.getHeight() * scaleShift));
        }

        return this.tooltipSize;
    }

    public void draw() {
        if (!isEmpty()) {
            final Point position = prepareTooltipPosition(this.mouseX, this.mouseY);
            draw(position.x, position.y);
        }
    }

    public void draw(int x, int y) {
        if (!isEmpty()) {
            this.renderer.draw(this, x, y);
        }
    }

    protected Point prepareTooltipPosition(int mouseX, int mouseY) {
        final ScaledResolution freeSpace = ClientUtil.getScaledResolution();
        final float scaleShift = (float) getScaleFactor() / freeSpace.getScaleFactor();
        final Dimension tooltipSize = getTooltipSize();
        final int offsetMain = (int) (this.renderer.getMainAxisOffset() * scaleShift);
        final int offsetCross = (int) (this.renderer.getCrossAxisOffset() * scaleShift);
        final Point rightPoint = new Point(mouseX + offsetMain, mouseY + offsetCross);

        if (rightPoint.x + tooltipSize.width <= freeSpace.getScaledWidth()) {
            rightPoint.y = clamp(rightPoint.y, 0, freeSpace.getScaledHeight() - tooltipSize.height);
            return rightPoint;
        }

        final Point leftPoint = new Point(mouseX - tooltipSize.width - offsetMain, mouseY + offsetCross);

        if (leftPoint.x >= 0) {
            leftPoint.y = clamp(leftPoint.y, 0, freeSpace.getScaledHeight() - tooltipSize.height);
            return leftPoint;
        }

        final Point bottomPoint = new Point(mouseX + offsetCross, mouseY + offsetMain);

        if (bottomPoint.y + tooltipSize.height <= freeSpace.getScaledHeight()) {
            bottomPoint.x = clamp(bottomPoint.x, 0, freeSpace.getScaledWidth() - tooltipSize.width);
            return bottomPoint;
        }

        final Point topPoint = new Point(mouseX + offsetCross, mouseY - tooltipSize.height - offsetMain);

        if (topPoint.y >= 0) {
            topPoint.x = clamp(topPoint.x, 0, freeSpace.getScaledWidth() - tooltipSize.width);
            return topPoint;
        }

        // more space on right side
        if (mouseX < freeSpace.getScaledWidth() - mouseX) {
            rightPoint.y = clamp(rightPoint.y, 0, freeSpace.getScaledHeight() - tooltipSize.height);
            rightPoint.x = clamp(rightPoint.x, 0, freeSpace.getScaledWidth() - tooltipSize.width);
            return rightPoint;
        } else {
            leftPoint.y = clamp(leftPoint.y, 0, freeSpace.getScaledHeight() - tooltipSize.height);
            leftPoint.x = clamp(leftPoint.x, 0, freeSpace.getScaledWidth() - tooltipSize.width);
            return leftPoint;
        }

    }

    protected int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

}
