package mekanism.client.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.providers.IItemProvider;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiTexturedElement;
import mekanism.client.gui.element.bar.GuiBar.IBarInfoHandler;
import mekanism.client.gui.element.gauge.GaugeOverlay;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.IProgressInfoHandler;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.common.MekanismLang;
import mekanism.common.util.text.TextUtils;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

public abstract class BaseRecipeCategory<RECIPE> implements IRecipeCategory<RECIPE>, IGuiWrapper {

    private static final IProgressInfoHandler CONSTANT_PROGRESS = () -> 1;
    protected static final IBarInfoHandler FULL_BAR = () -> 1;

    protected static IDrawable createIcon(IGuiHelper helper, ResourceLocation iconRL) {
        return helper.drawableBuilder(iconRL, 0, 0, 18, 18).setTextureSize(18, 18).build();
    }

    protected static IDrawable createIcon(IGuiHelper helper, IItemProvider provider) {
        return helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, provider.getItemStack());
    }

    private final List<GuiTexturedElement> guiElements = new ArrayList<>();
    private final Component component;
    private final IGuiHelper guiHelper;
    private final IDrawable background;
    private final RecipeType<RECIPE> recipeType;
    private final IDrawable icon;
    private final int xOffset;
    private final int yOffset;
    @Nullable
    private Map<GaugeOverlay, IDrawable> overlayLookup;
    @Nullable
    private ITickTimer timer;

    protected BaseRecipeCategory(IGuiHelper helper, MekanismJEIRecipeType<RECIPE> recipeType, IItemProvider provider, int xOffset, int yOffset, int width, int height) {
        this(helper, MekanismJEI.recipeType(recipeType), provider, xOffset, yOffset, width, height);
    }

    protected BaseRecipeCategory(IGuiHelper helper, RecipeType<RECIPE> recipeType, IItemProvider provider, int xOffset, int yOffset, int width, int height) {
        this(helper, recipeType, provider.getTextComponent(), createIcon(helper, provider), xOffset, yOffset, width, height);
    }

    protected BaseRecipeCategory(IGuiHelper helper, MekanismJEIRecipeType<RECIPE> recipeType, Component component, IDrawable icon, int xOffset, int yOffset, int width, int height) {
        this(helper, MekanismJEI.recipeType(recipeType), component, icon, xOffset, yOffset, width, height);
    }

    protected BaseRecipeCategory(IGuiHelper helper, RecipeType<RECIPE> recipeType, Component component, IDrawable icon, int xOffset, int yOffset, int width, int height) {
        this.recipeType = recipeType;
        this.component = component;
        this.guiHelper = helper;
        this.icon = icon;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.background = new NOOPDrawable(width, height);
    }

    protected <ELEMENT extends GuiTexturedElement> ELEMENT addElement(ELEMENT element) {
        guiElements.add(element);
        return element;
    }

    /**
     * @apiNote x and y are based on the values set in the tile, as the GUI then shifts the slots by one to account for the border. This method is mostly meant as a
     * helper to make keeping track of the positioning numbers easier.
     */
    protected GuiSlot addSlot(SlotType type, int x, int y) {
        return addElement(new GuiSlot(type, this, x - 1, y - 1));
    }

    protected GuiProgress addSimpleProgress(ProgressType type, int x, int y) {
        return addElement(new GuiProgress(getSimpleProgressTimer(), type, this, x, y));
    }

    protected GuiProgress addConstantProgress(ProgressType type, int x, int y) {
        return addElement(new GuiProgress(CONSTANT_PROGRESS, type, this, x, y));
    }

    @Override
    public int getGuiLeft() {
        return -xOffset;
    }

    @Override
    public int getGuiTop() {
        return -yOffset;
    }

    @Override
    public int getXSize() {
        return background.getWidth();
    }

    @Override
    public int getYSize() {
        return background.getHeight();
    }

    @Override
    public RecipeType<RECIPE> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return component;
    }

    @Override
    public void draw(RECIPE recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        //Translate back by our offset so that we are effectively rendering the foreground starting at 0, 0
        // This is needed to make sure that we render things like crystallizer text in the correct spot
        // If this ends up causing issues elsewhere we will need to look into it further
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(-xOffset, -yOffset, 0);
        renderElements(recipe, recipeSlotsView, guiGraphics, (int) mouseX, (int) mouseY);
        pose.popPose();
    }

    protected void renderElements(RECIPE recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, int x, int y) {
        PoseStack pose = guiGraphics.pose();
        guiElements.forEach(e -> e.renderShifted(guiGraphics, x, y, 0));
        guiElements.forEach(e -> e.onDrawBackground(guiGraphics, x, y, 0));
        //Note: We don't care that onRenderForeground updates the maxZOffset in the mekanism gui as that is just used for rendering windows
        // and as our categories don't support windows we don't need to worry about that
        int zOffset = 200;
        for (GuiTexturedElement element : guiElements) {
            pose.pushPose();
            element.onRenderForeground(guiGraphics, x, y, zOffset, zOffset);
            pose.popPose();
        }
    }

    @Override
    public Font getFont() {
        return Minecraft.getInstance().font;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    protected IProgressInfoHandler getSimpleProgressTimer() {
        if (timer == null) {
            timer = guiHelper.createTickTimer(20, 20, false);
        }
        return () -> timer.getValue() / 20D;
    }

    protected IBarInfoHandler getBarProgressTimer() {
        if (timer == null) {
            timer = guiHelper.createTickTimer(20, 20, false);
        }
        return new IBarInfoHandler() {
            @Override
            public Component getTooltip() {
                return MekanismLang.PROGRESS.translate(TextUtils.getPercent(getLevel()));
            }

            @Override
            public double getLevel() {
                return timer.getValue() / 20D;
            }
        };
    }

    private IDrawable getOverlay(GuiGauge<?> gauge) {
        if (overlayLookup == null) {
            overlayLookup = new EnumMap<>(GaugeOverlay.class);
        }
        return overlayLookup.computeIfAbsent(gauge.getGaugeOverlay(), overlay -> createDrawable(guiHelper, overlay));
    }

    private IDrawable createDrawable(IGuiHelper helper, GaugeOverlay gaugeOverlay) {
        return helper.drawableBuilder(gaugeOverlay.getBarOverlay(), 0, 0, gaugeOverlay.getWidth(), gaugeOverlay.getHeight())
              .setTextureSize(gaugeOverlay.getWidth(), gaugeOverlay.getHeight())
              .build();
    }

    protected <STACK> STACK getDisplayedStack(IRecipeSlotsView recipeSlotsView, String slotName, IIngredientType<STACK> type, STACK empty) {
        return recipeSlotsView.findSlotByName(slotName)
              .flatMap(view -> view.getDisplayedIngredient(type))
              .orElse(empty);
    }

    protected IRecipeSlotBuilder initItem(IRecipeLayoutBuilder builder, RecipeIngredientRole role, GuiSlot slot, List<ItemStack> stacks) {
        return initItem(builder, role, slot.getRelativeX(), slot.getRelativeY(), stacks);
    }

    protected IRecipeSlotBuilder initItem(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int relativeX, int relativeY, List<ItemStack> stacks) {
        return builder.addSlot(role, relativeX + 1 - xOffset, relativeY + 1 - yOffset)
              .addItemStacks(stacks);
    }

    protected IRecipeSlotBuilder initFluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, GuiGauge<?> gauge, List<FluidStack> stacks) {
        int width = gauge.getWidth() - 2;
        int height = gauge.getHeight() - 2;
        //If we have no max (no fluids or just an empty fluid) we want to ensure the fluid renderer doesn't throw errors,
        // so we just return a capacity for the render of a bucket
        int max = stacks.stream().mapToInt(FluidStack::getAmount).filter(stackSize -> stackSize > 0).max().orElse(FluidType.BUCKET_VOLUME);
        return init(builder, NeoForgeTypes.FLUID_STACK, role, gauge, stacks)
              .setFluidRenderer(max, false, width, height);
    }

    protected <STACK extends ChemicalStack<?>> IRecipeSlotBuilder initChemical(IRecipeLayoutBuilder builder, IIngredientType<STACK> type, RecipeIngredientRole role,
          GuiElement element, List<STACK> stacks) {
        int width = element.getWidth() - 2;
        int height = element.getHeight() - 2;
        //If we have no max (no chemicals or just an empty chemical) we mirror how we handle fluids and just return a capacity for the render of a bucket
        long max = stacks.stream().mapToLong(ChemicalStack::getAmount).filter(stackSize -> stackSize > 0).max().orElse(FluidType.BUCKET_VOLUME);
        return init(builder, type, role, element, stacks)
              .setCustomRenderer(type, new ChemicalStackRenderer<>(max, width, height));
    }

    private <STACK> IRecipeSlotBuilder init(IRecipeLayoutBuilder builder, IIngredientType<STACK> type, RecipeIngredientRole role, GuiElement element, List<STACK> stacks) {
        int x = element.getRelativeX() + 1 - xOffset;
        int y = element.getRelativeY() + 1 - yOffset;
        IRecipeSlotBuilder slotBuilder = builder.addSlot(role, x, y)
              .addIngredients(type, stacks);
        if (element instanceof GuiGauge<?> gauge) {
            slotBuilder.setOverlay(getOverlay(gauge), 0, 0);
        }
        return slotBuilder;
    }
}