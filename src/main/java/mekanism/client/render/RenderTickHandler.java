package mekanism.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import mekanism.api.RelativeSide;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.GuiRadialSelector;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.client.render.RenderResizableCuboid.FaceDisplay;
import mekanism.client.render.armor.ISpecialGear;
import mekanism.client.render.armor.MekaSuitArmor;
import mekanism.client.render.hud.RadiationOverlay;
import mekanism.client.render.lib.Quad;
import mekanism.client.render.lib.QuadUtils;
import mekanism.client.render.lib.Vertex;
import mekanism.client.render.lib.effect.BoltRenderer;
import mekanism.client.render.tileentity.IWireFrameRenderer;
import mekanism.common.Mekanism;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.block.BlockBounding;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeCustomSelectionBox;
import mekanism.common.content.gear.IBlastingItem;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.item.ItemConfigurator.ConfiguratorMode;
import mekanism.common.item.gear.ItemFlamethrower;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import mekanism.common.lib.effect.BoltEffect;
import mekanism.common.lib.math.Pos3D;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismParticleTypes;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.interfaces.ISideConfiguration;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.WorldUtils;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.event.TickEvent.ClientTickEvent;
import net.neoforged.neoforge.event.TickEvent.Phase;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class RenderTickHandler {

    public final Minecraft minecraft = Minecraft.getInstance();

    private static final Map<BlockState, List<Vertex[]>> cachedWireFrames = new HashMap<>();
    private static final Map<Direction, Map<TransmissionType, Model3D>> cachedOverlays = new EnumMap<>(Direction.class);
    private static final Map<RenderType, List<LazyRender>> transparentRenderers = new HashMap<>();
    private static final BoltRenderer boltRenderer = new BoltRenderer();

    private boolean outliningArea = false;

    public static void clearQueued() {
        RadiationOverlay.INSTANCE.resetRadiation();
        transparentRenderers.clear();
    }

    public static void resetCached() {
        cachedOverlays.clear();
        cachedWireFrames.clear();
    }

    public static void renderBolt(Object renderer, BoltEffect bolt) {
        boltRenderer.update(renderer, bolt, MekanismRenderer.getPartialTick());
    }

    //Note: This listener is only registered if a recipe viewer is loaded
    public static void guiOpening(ScreenEvent.Opening event) {
        if (event.getCurrentScreen() instanceof GuiMekanism<?> screen) {
            if (Mekanism.hooks.JEILoaded) {
                //If JEI is loaded and our current screen is a mekanism gui, check if the new screen is a JEI recipe screen
                if (event.getNewScreen() instanceof IRecipesGui) {
                    //If it is mark on our current screen that we are switching to JEI
                    screen.switchingToRecipeViewer = true;
                }
            }
            if (Mekanism.hooks.EmiLoaded) {
                //If Emi is loaded and our current screen is a mekanism gui, check if the new screen is an Emi recipe screen
                //TODO - 1.20.4: Figure out a better way to handle this https://github.com/emilyploszaj/emi/issues/481
                if (event.getNewScreen() != null && event.getNewScreen().getClass().getPackageName().startsWith("dev.emi.emi")) {
                    //If it is mark on our current screen that we are switching to EMI
                    screen.switchingToRecipeViewer = true;
                }
            }
        }
    }

    public static void addTransparentRenderer(RenderType renderType, LazyRender render) {
        transparentRenderers.computeIfAbsent(renderType, r -> new ArrayList<>()).add(render);
    }

    @SubscribeEvent
    public void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            //Only do matrix transforms and mess with buffers if we actually have any renders to render
            renderStage(event, !transparentRenderers.isEmpty(), (camera, renderer, poseStack, renderTick, partialTick) -> {
                ProfilerFiller profiler = minecraft.getProfiler();
                profiler.push(ProfilerConstants.DELAYED);
                record TransparentRenderInfo(RenderType renderType, List<LazyRender> renders, double closest) {
                }
                Consumer<TransparentRenderInfo> renderInfoConsumer = info -> {
                    //Batch all renders for a single render type into a single buffer addition
                    VertexConsumer buffer = renderer.getBuffer(info.renderType);
                    for (LazyRender transparentRender : info.renders) {
                        String profilerSection = transparentRender.getProfilerSection();
                        if (profilerSection != null) {
                            profiler.push(profilerSection);
                        }
                        //Note: We don't bother sorting renders in a specific render type as we assume the render type has sortOnUpload as true
                        transparentRender.render(camera, buffer, poseStack, renderTick, partialTick, profiler);
                        if (profilerSection != null) {
                            profiler.pop();
                        }
                    }
                    renderer.endBatch(info.renderType);
                };
                if (transparentRenderers.size() == 1) {
                    //If we only have one render type we don't need to bother calculating any distances
                    for (Map.Entry<RenderType, List<LazyRender>> entry : transparentRenderers.entrySet()) {
                        renderInfoConsumer.accept(new TransparentRenderInfo(entry.getKey(), entry.getValue(), 0));
                    }
                } else {
                    List<TransparentRenderInfo> toSort = new ArrayList<>(transparentRenderers.size());
                    for (Entry<RenderType, List<LazyRender>> renderTypeListEntry : transparentRenderers.entrySet()) {
                        List<LazyRender> renders = renderTypeListEntry.getValue();
                        double closest = Double.MAX_VALUE;
                        for (LazyRender render : renders) {
                            Vec3 renderPos = render.getCenterPos(partialTick);
                            if (renderPos != null) {
                                //Note: We can just use the distance sqr as we use it for both things, so they compare the same anyway
                                double distanceSqr = camera.getPosition().distanceToSqr(renderPos);
                                if (distanceSqr < closest) {
                                    closest = distanceSqr;
                                }
                            }
                        }
                        //Note: we remap it in order to keep track of the closest distance so that we only have to calculate it once
                        toSort.add(new TransparentRenderInfo(renderTypeListEntry.getKey(), renders, closest));
                    }
                    //Sort in the order of furthest to closest (reverse of by closest)
                    toSort.sort(Comparator.comparingDouble(info -> -info.closest));
                    for (TransparentRenderInfo apply : toSort) {
                        renderInfoConsumer.accept(apply);
                    }
                }
                transparentRenderers.clear();
                profiler.pop();
            });
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES && boltRenderer.hasBoltsToRender()) {
            //Only do matrix transforms and mess with buffers if we actually have any bolts to render
            renderStage(event, boltRenderer.hasBoltsToRender(), (camera, renderer, poseStack, renderTick, partialTick) -> {
                boltRenderer.render(partialTick, poseStack, renderer, camera.getPosition());
                renderer.endBatch(MekanismRenderType.MEK_LIGHTNING);
            });
        }
    }

    private void renderStage(RenderLevelStageEvent event, boolean shouldRender, StageRenderer renderer) {
        if (shouldRender) {
            renderer.render(event.getCamera(), minecraft.renderBuffers().bufferSource(), event.getPoseStack(), event.getRenderTick(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public void renderCrosshair(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type() && minecraft.screen instanceof GuiRadialSelector screen && screen.shouldHideCrosshair()) {
            //Hide the crosshair if we have a radial menu open and are drawing the back button
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void renderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() instanceof ItemMekaSuitArmor armorItem) {
            MekaSuitArmor armor = (MekaSuitArmor) ((ISpecialGear) IClientItemExtensions.of(armorItem)).getGearModel(ArmorItem.Type.CHESTPLATE);
            PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            PlayerModel<AbstractClientPlayer> model = renderer.getModel();
            model.setAllVisible(true);
            //Note: We just want it to act as empty even if there is a map as it looks a lot better
            boolean rightHand = event.getArm() == HumanoidArm.RIGHT;
            if (rightHand) {
                model.rightArmPose = ArmPose.EMPTY;
            } else {
                model.leftArmPose = ArmPose.EMPTY;
            }
            model.attackTime = 0.0F;
            model.crouching = false;
            model.swimAmount = 0.0F;
            model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            armor.renderArm(model, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, player, chestStack, rightHand);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void tickEnd(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            //Note: We check that the game mode is not null as if it is that means the world is unloading, and we don't actually want to be rendering
            // as our data may be out of date or invalid. For example configs could unload while it is still unloading
            if (minecraft.player != null && minecraft.player.level() != null && !minecraft.isPaused() && minecraft.gameMode != null) {
                Player player = minecraft.player;
                Level world = minecraft.player.level();
                //Traverse active jetpacks and do animations
                for (UUID uuid : Mekanism.playerState.getActiveJetpacks()) {
                    Player p = world.getPlayerByUUID(uuid);
                    if (p != null) {
                        Pos3D playerPos = new Pos3D(p).translate(0, p.getEyeHeight(), 0);
                        Vec3 playerMotion = p.getDeltaMovement();
                        float random = (world.random.nextFloat() - 0.5F) * 0.1F;
                        //This positioning code is somewhat cursed, but it seems to be mostly working and entity pose code seems cursed in general
                        float xRot;
                        if (p.isCrouching()) {
                            xRot = 20;
                            playerPos = playerPos.translate(0, 0.125, 0);
                        } else {
                            float f = p.getSwimAmount(minecraft.getPartialTick());
                            if (p.isFallFlying()) {
                                float f1 = (float) p.getFallFlyingTicks() + minecraft.getPartialTick();
                                float f2 = Mth.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
                                xRot = f2 * (-90.0F - p.getXRot());
                            } else {
                                float f3 = p.isInWater() ? -90.0F - p.getXRot() : -90.0F;
                                xRot = Mth.lerp(f, 0.0F, f3);
                            }
                            xRot = -xRot;
                            Pos3D eyeAdjustments;
                            if (p.isFallFlying() && (p != player || !minecraft.options.getCameraType().isFirstPerson())) {
                                eyeAdjustments = new Pos3D(0, p.getEyeHeight(Pose.STANDING), 0).xRot(xRot).yRot(p.yBodyRot);
                            } else if (p.isVisuallySwimming()) {
                                eyeAdjustments = new Pos3D(0, p.getEyeHeight(), 0).xRot(xRot).yRot(p.yBodyRot).translate(0, 0.5, 0);
                            } else {
                                eyeAdjustments = new Pos3D(0, p.getEyeHeight(), 0).xRot(xRot).yRot(p.yBodyRot);
                            }
                            playerPos = new Pos3D(p.getX() + eyeAdjustments.x, p.getY() + eyeAdjustments.y, p.getZ() + eyeAdjustments.z);
                        }
                        Pos3D vLeft = new Pos3D(-0.43, -0.55, -0.54).xRot(xRot).yRot(p.yBodyRot);
                        renderJetpackSmoke(world, playerPos.translate(vLeft, playerMotion), vLeft.scale(0.2).translate(playerMotion, vLeft.scale(random)));
                        Pos3D vRight = new Pos3D(0.43, -0.55, -0.54).xRot(xRot).yRot(p.yBodyRot);
                        renderJetpackSmoke(world, playerPos.translate(vRight, playerMotion), vRight.scale(0.2).translate(playerMotion, vRight.scale(random)));
                        Pos3D vCenter = new Pos3D((world.random.nextFloat() - 0.5) * 0.4, -0.86, -0.30).xRot(xRot).yRot(p.yBodyRot);
                        renderJetpackSmoke(world, playerPos.translate(vCenter, playerMotion), vCenter.scale(0.2).translate(playerMotion));
                    }
                }

                if (world.getGameTime() % 4 == 0) {
                    //Traverse active scuba masks and do animations
                    for (UUID uuid : Mekanism.playerState.getActiveScubaMasks()) {
                        Player p = world.getPlayerByUUID(uuid);
                        if (p != null && p.isInWater()) {
                            Pos3D vec = new Pos3D(0.4, 0.4, 0.4).multiply(p.getViewVector(1)).translate(0, -0.2, 0);
                            Pos3D motion = vec.scale(0.2).translate(p.getDeltaMovement());
                            Pos3D v = new Pos3D(p).translate(0, p.getEyeHeight(), 0).translate(vec);
                            world.addParticle(MekanismParticleTypes.SCUBA_BUBBLE.get(), v.x, v.y, v.z, motion.x, motion.y + 0.2, motion.z);
                        }
                    }
                    //Traverse players and do animations for idle flamethrowers
                    for (Player p : world.players()) {
                        if (!p.swinging) {
                            if (player.isUsingItem()) {
                                InteractionHand usedHand = player.getUsedItemHand();
                                if (!(player.getItemInHand(usedHand).getItem() instanceof ItemFlamethrower)) {
                                    //If we the used item isn't a flamethrower, grab the other hand's item for checks
                                    // if it was an active flamethrower we just skip adding the idle particles
                                    tryAddIdleFlamethrowerParticles(minecraft, p, usedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                                }
                            } else if (!tryAddIdleFlamethrowerParticles(minecraft, p, InteractionHand.MAIN_HAND)) {
                                //If the player isn't using an item, try to first add particles for a flamethrower in the main hand
                                // and then add particles for a flamethrower in the offhand if we failed
                                tryAddIdleFlamethrowerParticles(minecraft, p, InteractionHand.OFF_HAND);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean tryAddIdleFlamethrowerParticles(Minecraft minecraft, Player player, InteractionHand hand) {
        if (!ItemFlamethrower.isIdleFlamethrower(player, hand)) {
            return false;
        }
        Pos3D flameVec;
        Entity vehicle = player.getVehicle();
        boolean rightHanded = MekanismUtils.isRightArm(player, hand);
        if (minecraft.player == player && minecraft.options.getCameraType().isFirstPerson()) {
            flameVec = new Pos3D(1, 1, 1)
                  .multiply(player.getViewVector(minecraft.getPartialTick()))
                  .yRot(rightHanded ? 15 : -15)
                  .translate(0, player.getEyeHeight() - 0.1, 0);
        } else {
            double flameXCoord = rightHanded ? -0.2 : 0.2;
            double flameYCoord = 1;
            double flameZCoord = 1.2;
            if (player.isCrouching()) {
                flameYCoord -= 0.65;
                flameZCoord -= 0.15;
            } else if (vehicle != null) {
                flameYCoord -= player.getMyRidingOffset(vehicle) + 0.1;
            }
            flameVec = new Pos3D(flameXCoord, flameYCoord, flameZCoord).yRot(player.yBodyRot);
        }
        Vec3 motion = vehicle == null ? player.getDeltaMovement() : vehicle.getDeltaMovement();
        Vec3 flameMotion = new Vec3(motion.x(), player.onGround() || vehicle != null ? 0 : motion.y(), motion.z());
        Vec3 mergedVec = player.position().add(flameVec);
        player.level().addParticle(MekanismParticleTypes.JETPACK_FLAME.get(), mergedVec.x, mergedVec.y, mergedVec.z, flameMotion.x, flameMotion.y, flameMotion.z);
        return true;
    }

    @SubscribeEvent
    public void onBlockHover(RenderHighlightEvent.Block event) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        BlockHitResult rayTraceResult = event.getTarget();
        if (rayTraceResult.getType() != Type.MISS) {
            Level world = player.level();
            BlockPos pos = rayTraceResult.getBlockPos();
            MultiBufferSource renderer = event.getMultiBufferSource();
            Camera info = event.getCamera();
            PoseStack matrix = event.getPoseStack();
            ProfilerFiller profiler = world.getProfiler();
            BlockState blockState = world.getBlockState(pos);

            profiler.push(ProfilerConstants.AREA_MINE_OUTLINE);
            // Draw outlines for area mining blocks
            if (!outliningArea) {
                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty() && stack.getItem() instanceof IBlastingItem tool) {
                    Map<BlockPos, BlockState> blocks = tool.getBlastedBlocks(world, player, stack, pos, blockState);
                    if (!blocks.isEmpty()) {
                        outliningArea = true;
                        Vec3 renderView = info.getPosition();
                        LevelRenderer levelRenderer = event.getLevelRenderer();
                        Lazy<VertexConsumer> lineConsumer = Lazy.of(() -> renderer.getBuffer(RenderType.lines()));
                        for (Entry<BlockPos, BlockState> block : blocks.entrySet()) {
                            BlockPos blastingTarget = block.getKey();
                            if (!pos.equals(blastingTarget) && !ClientHooks.onDrawHighlight(levelRenderer, info, rayTraceResult, event.getPartialTick(), matrix, renderer)) {
                                levelRenderer.renderHitOutline(matrix, lineConsumer.get(), player, renderView.x, renderView.y, renderView.z, blastingTarget, block.getValue());
                            }
                        }
                        outliningArea = false;
                    }
                }
            }
            profiler.pop();

            boolean shouldCancel = false;
            profiler.push(ProfilerConstants.MEKANISM_OUTLINE);
            if (!blockState.isAir() && world.getWorldBorder().isWithinBounds(pos)) {
                BlockPos actualPos = pos;
                BlockState actualState = blockState;
                if (blockState.getBlock() instanceof BlockBounding) {
                    TileEntityBoundingBlock tile = WorldUtils.getTileEntity(TileEntityBoundingBlock.class, world, pos);
                    if (tile != null && tile.hasReceivedCoords()) {
                        actualPos = tile.getMainPos();
                        actualState = world.getBlockState(actualPos);
                    }
                }
                AttributeCustomSelectionBox customSelectionBox = Attribute.get(actualState, AttributeCustomSelectionBox.class);
                if (customSelectionBox != null) {
                    WireFrameRenderer renderWireFrame = null;
                    if (customSelectionBox.isJavaModel()) {
                        //If we use a TER to render the wire frame, grab the tile
                        BlockEntity tile = WorldUtils.getTileEntity(world, actualPos);
                        if (tile != null) {
                            BlockEntityRenderer<BlockEntity> tileRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tile);
                            if (tileRenderer instanceof IWireFrameRenderer wireFrameRenderer && wireFrameRenderer.hasSelectionBox(actualState)) {
                                renderWireFrame = (buffer, matrixStack, state, red, green, blue, alpha) -> {
                                    if (wireFrameRenderer.isCombined()) {
                                        renderQuadsWireFrame(state, buffer, matrixStack.last().pose(), world.random, red, green, blue, alpha);
                                    }
                                    wireFrameRenderer.renderWireFrame(tile, event.getPartialTick(), matrixStack, buffer, red, green, blue, alpha);
                                };
                            }
                        }
                    } else {
                        //Otherwise, skip getting the tile and just grab the model
                        renderWireFrame = (buffer, matrixStack, state, red, green, blue, alpha) ->
                              renderQuadsWireFrame(state, buffer, matrixStack.last().pose(), world.random, red, green, blue, alpha);
                    }
                    if (renderWireFrame != null) {
                        matrix.pushPose();
                        Vec3 viewPosition = info.getPosition();
                        matrix.translate(actualPos.getX() - viewPosition.x, actualPos.getY() - viewPosition.y, actualPos.getZ() - viewPosition.z);
                        //0.4 Alpha
                        renderWireFrame.render(renderer.getBuffer(RenderType.lines()), matrix, actualState, 0, 0, 0, 0x66);
                        matrix.popPose();
                        shouldCancel = true;
                    }
                }
            }
            profiler.pop();

            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemConfigurator)) {
                //If we are not holding a configurator, look if we are in the offhand
                stack = player.getOffhandItem();
                if (stack.isEmpty() || !(stack.getItem() instanceof ItemConfigurator)) {
                    if (shouldCancel) {
                        event.setCanceled(true);
                    }
                    return;
                }
            }
            profiler.push(ProfilerConstants.CONFIGURABLE_MACHINE);
            ConfiguratorMode state = ((ItemConfigurator) stack.getItem()).getMode(stack);
            if (state.isConfigurating()) {
                TransmissionType type = Objects.requireNonNull(state.getTransmission(), "Configurating state requires transmission type");
                BlockEntity tile = WorldUtils.getTileEntity(world, pos);
                if (tile instanceof ISideConfiguration configurable) {
                    TileComponentConfig config = configurable.getConfig();
                    if (config.supports(type)) {
                        Direction face = rayTraceResult.getDirection();
                        DataType dataType = config.getDataType(type, RelativeSide.fromDirections(configurable.getDirection(), face));
                        if (dataType != null) {
                            Vec3 viewPosition = info.getPosition();
                            matrix.pushPose();
                            matrix.translate(pos.getX() - viewPosition.x, pos.getY() - viewPosition.y, pos.getZ() - viewPosition.z);
                            MekanismRenderer.renderObject(getOverlayModel(face, type), matrix, renderer.getBuffer(Sheets.translucentCullBlockSheet()),
                                  MekanismRenderer.getColorARGB(dataType.getColor(), 0.6F), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, FaceDisplay.FRONT,
                                  info);
                            matrix.popPose();
                        }
                    }
                }
            }
            profiler.pop();
            if (shouldCancel) {
                event.setCanceled(true);
            }
        }
    }

    private void renderQuadsWireFrame(BlockState state, VertexConsumer buffer, Matrix4f matrix, RandomSource rand, int red, int green, int blue, int alpha) {
        List<Vertex[]> allVertices = cachedWireFrames.computeIfAbsent(state, s -> {
            BakedModel bakedModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(s);
            //TODO: Eventually we may want to add support for Model data and maybe render type
            ModelData modelData = ModelData.EMPTY;
            List<Vertex[]> vertices = new ArrayList<>();
            for (Direction direction : EnumUtils.DIRECTIONS) {
                QuadUtils.unpack(bakedModel.getQuads(s, direction, rand, modelData, null)).stream().map(Quad::getVertices).forEach(vertices::add);
            }
            QuadUtils.unpack(bakedModel.getQuads(s, null, rand, modelData, null)).stream().map(Quad::getVertices).forEach(vertices::add);
            return vertices;
        });
        renderVertexWireFrame(allVertices, buffer, matrix, red, green, blue, alpha);
    }

    public static void renderVertexWireFrame(List<Vertex[]> allVertices, VertexConsumer buffer, Matrix4f matrix, int red, int green, int blue, int alpha) {
        for (Vertex[] vertices : allVertices) {
            Vector4f vertex = getVertex(matrix, vertices[0]);
            Vector3f normal = vertices[0].getNormal();
            Vector4f vertex2 = getVertex(matrix, vertices[1]);
            Vector3f normal2 = vertices[1].getNormal();
            Vector4f vertex3 = getVertex(matrix, vertices[2]);
            Vector3f normal3 = vertices[2].getNormal();
            Vector4f vertex4 = getVertex(matrix, vertices[3]);
            Vector3f normal4 = vertices[3].getNormal();
            buffer.vertex(vertex.x(), vertex.y(), vertex.z()).color(red, green, blue, alpha).normal(normal.x(), normal.y(), normal.z()).endVertex();
            buffer.vertex(vertex2.x(), vertex2.y(), vertex2.z()).color(red, green, blue, alpha).normal(normal2.x(), normal2.y(), normal2.z()).endVertex();

            buffer.vertex(vertex3.x(), vertex3.y(), vertex3.z()).color(red, green, blue, alpha).normal(normal3.x(), normal3.y(), normal3.z()).endVertex();
            buffer.vertex(vertex4.x(), vertex4.y(), vertex4.z()).color(red, green, blue, alpha).normal(normal4.x(), normal4.y(), normal4.z()).endVertex();

            buffer.vertex(vertex2.x(), vertex2.y(), vertex2.z()).color(red, green, blue, alpha).normal(normal2.x(), normal2.y(), normal2.z()).endVertex();
            buffer.vertex(vertex3.x(), vertex3.y(), vertex3.z()).color(red, green, blue, alpha).normal(normal3.x(), normal3.y(), normal3.z()).endVertex();

            buffer.vertex(vertex.x(), vertex.y(), vertex.z()).color(red, green, blue, alpha).normal(normal.x(), normal.y(), normal.z()).endVertex();
            buffer.vertex(vertex4.x(), vertex4.y(), vertex4.z()).color(red, green, blue, alpha).normal(normal4.x(), normal4.y(), normal4.z()).endVertex();
        }
    }

    private static Vector4f getVertex(Matrix4f matrix4f, Vertex vertex) {
        Vector4f vector4f = new Vector4f((float) vertex.getPos().x(), (float) vertex.getPos().y(), (float) vertex.getPos().z(), 1);
        return vector4f.mul(matrix4f);
    }

    private void renderJetpackSmoke(Level world, Vec3 pos, Vec3 motion) {
        world.addParticle(MekanismParticleTypes.JETPACK_FLAME.get(), pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
        world.addParticle(MekanismParticleTypes.JETPACK_SMOKE.get(), pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
    }

    private Model3D getOverlayModel(Direction side, TransmissionType type) {
        return cachedOverlays.computeIfAbsent(side, s -> new EnumMap<>(TransmissionType.class))
              .computeIfAbsent(type, t -> new Model3D()
                    .setTexture(MekanismRenderer.overlays.get(t))
                    .prepSingleFaceModelSize(side)
              );
    }

    @FunctionalInterface
    private interface StageRenderer {

        /**
         * @param camera Camera position, in general rendering will have to be translated by the inverse position of the client viewing camera to get back to 0, 0, 0
         */
        void render(Camera camera, MultiBufferSource.BufferSource renderer, PoseStack poseStack, int renderTick, float partialTick);
    }

    @FunctionalInterface
    public interface LazyRender {

        void render(Camera camera, VertexConsumer buffer, PoseStack poseStack, int renderTick, float partialTick, ProfilerFiller profiler);

        @Nullable
        default Vec3 getCenterPos(float partialTick) {
            return null;
        }

        @Nullable
        default String getProfilerSection() {
            return null;
        }
    }

    @FunctionalInterface
    private interface WireFrameRenderer {

        void render(VertexConsumer buffer, PoseStack matrix, BlockState state, int red, int green, int blue, int alpha);
    }
}