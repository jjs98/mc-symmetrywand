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
        if (plane.axes().octagonal()) {
            addDiagonalQuad(buffer, matrix, plane, true, 1f, 1f, 0f);
            addDiagonalQuad(buffer, matrix, plane, false, 1f, 0f, 1f);
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

    /** Renders the main (x-z=const) or anti (x+z=const) diagonal plane through its exact center,
     * which may sit between two diagonally-adjacent blocks rather than centered on one. */
    private static void addDiagonalQuad(BufferBuilder buffer, Matrix4f matrix, SymmetryPlane plane, boolean main, float r, float g, float b) {
        float cx = center(plane.sumX());
        float cy = center(plane.sumY());
        // for continuous block centers, x - z = (sumX - sumZ) / 2 exactly (the "+1"s from
        // center() cancel out on subtraction), but x + z = (sumX + sumZ) / 2 + 1 (they add up
        // instead of canceling) - using the un-adjusted sum here would offset the anti-diagonal
        // plane by a full block
        float cz = main
                ? cx - (plane.sumX() - plane.sumZ()) / 2.0f
                : (plane.sumX() + plane.sumZ()) / 2.0f + 1f - cx;
        float half = HALF_SIZE + 0.5f;
        float dirZ = main ? 1f : -1f;

        float x1 = cx - half;
        float z1 = cz - half * dirZ;
        float x2 = cx + half;
        float z2 = cz + half * dirZ;

        vertex(buffer, matrix, x1, cy - half, z1, r, g, b);
        vertex(buffer, matrix, x2, cy - half, z2, r, g, b);
        vertex(buffer, matrix, x2, cy + half, z2, r, g, b);
        vertex(buffer, matrix, x1, cy + half, z1, r, g, b);
    }

    /** The true midpoint of the two selected points along one axis; may be a half-block. */
    private static float center(int sum) {
        return (sum + 1) / 2.0f;
    }
}
