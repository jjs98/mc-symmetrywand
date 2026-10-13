package com.symmetrywand.item;

import com.symmetrywand.config.Config;
import com.symmetrywand.symmetry.SymmetryPlane;
import com.symmetrywand.symmetry.WandData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Sets, moves, reorients or resets a {@link SymmetryPlane} stored on the wand stack itself.
 * The actual mirroring of block placement/removal is handled by the event handlers in
 * {@code com.symmetrywand.event}, which look up this plane from any wand in the player's inventory.
 */
public class SymmetryWandItem extends Item {

    public SymmetryWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide() || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        int range = Config.MAX_RANGE.get();

        if (!player.blockPosition().closerThan(clicked, range)) {
            warn(player, "That block is too far away for the symmetry wand (max " + range + " blocks).");
            return InteractionResult.FAIL;
        }

        if (player.isShiftKeyDown()) {
            cycleOrientation(player, stack);
            return InteractionResult.SUCCESS;
        }

        WandData.PendingPoint pending = WandData.getPending(stack);

        if (pending == null) {
            Direction.Axis axis = WandData.getAxis(stack);
            WandData.setPending(stack, axis, clicked);
            info(player, "First point set at " + describe(clicked)
                    + " (axis: " + axis.getName().toUpperCase() + "). Right-click another block to set the plane between them, or click into the air to cancel.");
            return InteractionResult.SUCCESS;
        }

        WandData.clearPending(stack);
        BlockPos first = pending.pos();

        if (clicked.equals(first)) {
            WandData.set(stack, SymmetryPlane.centeredAt(pending.axis(), clicked));
            info(player, "Symmetry plane set at " + describe(clicked) + " (axis: " + pending.axis().getName().toUpperCase() + ").");
            return InteractionResult.SUCCESS;
        }

        int maxDistance = Config.MAX_SELECTION_DISTANCE.get();
        if (distance(first, clicked) > maxDistance) {
            warn(player, "Those two points are too far apart for the symmetry wand (max " + maxDistance + " blocks). Selection canceled.");
            return InteractionResult.FAIL;
        }

        WandData.set(stack, SymmetryPlane.between(pending.axis(), first, clicked));
        info(player, "Symmetry plane set between " + describe(first) + " and " + describe(clicked)
                + " (axis: " + pending.axis().getName().toUpperCase() + ").");
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player playerEntity, InteractionHand hand) {
        ItemStack stack = playerEntity.getItemInHand(hand);
        if (level.isClientSide() || !(playerEntity instanceof ServerPlayer player)) {
            return InteractionResultHolder.success(stack);
        }

        if (player.isShiftKeyDown()) {
            cycleOrientation(player, stack);
            return InteractionResultHolder.success(stack);
        }

        if (WandData.getPending(stack) != null) {
            WandData.clearPending(stack);
            info(player, "Selection canceled.");
            return InteractionResultHolder.success(stack);
        }

        if (WandData.get(stack) != null) {
            WandData.clear(stack);
            info(player, "Symmetry plane reset.");
        } else {
            info(player, "No symmetry plane is set.");
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.symmetrywand.symmetry_wand.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Axis: " + WandData.getAxis(stack).getName().toUpperCase()).withStyle(ChatFormatting.DARK_GRAY));
        SymmetryPlane plane = WandData.get(stack);
        if (plane != null) {
            tooltip.add(Component.literal("Plane @ " + describePlanePosition(plane)).withStyle(ChatFormatting.DARK_GRAY));
        }
        WandData.PendingPoint pending = WandData.getPending(stack);
        if (pending != null) {
            tooltip.add(Component.literal("First point: " + describe(pending.pos())).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private void cycleOrientation(ServerPlayer player, ItemStack stack) {
        Direction.Axis newAxis = SymmetryPlane.cycle(WandData.getAxis(stack));
        WandData.setAxis(stack, newAxis);

        SymmetryPlane plane = WandData.get(stack);
        if (plane != null) {
            WandData.set(stack, plane.withAxis(newAxis));
        }

        info(player, "Symmetry wand orientation changed to axis " + newAxis.getName().toUpperCase() + ".");
    }

    private static String describe(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    /** Shows the anchor's two non-plane coordinates plus the exact (possibly half-block) plane coordinate. */
    private static String describePlanePosition(SymmetryPlane plane) {
        double planeCoord = plane.planeCoord();
        BlockPos a = plane.anchor();
        return switch (plane.axis()) {
            case X -> planeCoord + ", " + a.getY() + ", " + a.getZ();
            case Y -> a.getX() + ", " + planeCoord + ", " + a.getZ();
            case Z -> a.getX() + ", " + a.getY() + ", " + planeCoord;
        };
    }

    private static double distance(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    private void info(ServerPlayer player, String message) {
        sendFeedback(player, Component.literal(message), ChatFormatting.GREEN);
    }

    private void warn(ServerPlayer player, String message) {
        sendFeedback(player, Component.literal(message), ChatFormatting.RED);
    }

    private void sendFeedback(ServerPlayer player, Component message, ChatFormatting color) {
        if (!Config.CHAT_FEEDBACK.get()) {
            return;
        }
        player.sendSystemMessage(message.copy().withStyle(color));
    }
}
