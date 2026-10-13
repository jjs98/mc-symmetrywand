package com.symmetrywand.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.symmetrywand.SymmetryWandMod;
import com.symmetrywand.item.SymmetryWandItem;
import com.symmetrywand.symmetry.SymmetryPlane;
import com.symmetrywand.symmetry.WandData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Draws a translucent, axis-colored sheet for each active axis of any Symmetry Wand carried in
 * the player's inventory (red = X axis, green = Y axis, blue = Z axis), so the plane(s) position
 * and orientation are visible in the world. The wand does not need to be held.
 */
@EventBusSubscriber(modid = SymmetryWandMod.MODID, value = Dist.CLIENT)
public final class SymmetryPlaneRenderer {

    private static final float HALF_SIZE = 2.0f;
    private static final float ALPHA = 0.35f;

    private SymmetryPlaneRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        SymmetryPlane plane = findActivePlane(player);
        if (plane == null) {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        renderPlane(poseStack, plane);
        poseStack.popPose();
    }

    private static SymmetryPlane findActivePlane(Player player) {
        SymmetryPlane plane = planeOf(player.getMainHandItem());
        if (plane != null) {
            return plane;
        }
        plane = planeOf(player.getOffhandItem());
        if (plane != null) {
            return plane;
        }
        for (ItemStack stack : player.getInventory().items) {
            plane = planeOf(stack);
            if (plane != null) {
                return plane;
            }
        }
        return null;
    }

    private static SymmetryPlane planeOf(ItemStack stack) {
        if (!(stack.getItem() instanceof SymmetryWandItem)) {
            return null;
        }
        return WandData.get(stack);
    }

    private static void renderPlane(PoseStack poseStack, SymmetryPlane plane) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Direction.Axis axis : plane.activeAxes()) {
            float r = axis == Direction.Axis.X ? 1f : 0f;
            float g = axis == Direction.Axis.Y ? 1f : 0f;
            float b = axis == Direction.Axis.Z ? 1f : 0f;
            addPlaneQuad(buffer, matrix, plane, axis, r, g, b);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void addPlaneQuad(BufferBuilder buffer, Matrix4f matrix, SymmetryPlane plane, Direction.Axis axis, float r, float g, float b) {
        float cx = center(plane.sumX());
        float cy = center(plane.sumY());
        float cz = center(plane.sumZ());
        float half = HALF_SIZE + 0.5f;
        float lo = -half;
        float hi = half;

        switch (axis) {
            case X -> {
                float planeCoord = cx;
                vertex(buffer, matrix, planeCoord, cy + lo, cz + lo, r, g, b);
                vertex(buffer, matrix, planeCoord, cy + hi, cz + lo, r, g, b);
                vertex(buffer, matrix, planeCoord, cy + hi, cz + hi, r, g, b);
                vertex(buffer, matrix, planeCoord, cy + lo, cz + hi, r, g, b);
            }
            case Y -> {
                float planeCoord = cy;
                vertex(buffer, matrix, cx + lo, planeCoord, cz + lo, r, g, b);
                vertex(buffer, matrix, cx + hi, planeCoord, cz + lo, r, g, b);
                vertex(buffer, matrix, cx + hi, planeCoord, cz + hi, r, g, b);
                vertex(buffer, matrix, cx + lo, planeCoord, cz + hi, r, g, b);
            }
            case Z -> {
                float planeCoord = cz;
                vertex(buffer, matrix, cx + lo, cy + lo, planeCoord, r, g, b);
                vertex(buffer, matrix, cx + hi, cy + lo, planeCoord, r, g, b);
                vertex(buffer, matrix, cx + hi, cy + hi, planeCoord, r, g, b);
                vertex(buffer, matrix, cx + lo, cy + hi, planeCoord, r, g, b);
            }
        }
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
                                float r, float g, float b) {
        buffer.addVertex(matrix, x, y, z).setColor(r, g, b, ALPHA);
    }

    /** The true midpoint of the two selected points along one axis; may be a half-block. */
    private static float center(int sum) {
        return (sum + 1) / 2.0f;
    }
}
