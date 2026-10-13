package com.symmetrywand.event;

import com.symmetrywand.SymmetryWandMod;
import com.symmetrywand.config.Config;
import com.symmetrywand.item.SymmetryWandItem;
import com.symmetrywand.symmetry.SymmetryPlane;
import com.symmetrywand.symmetry.WandData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Mirrors block placement and removal across the active symmetry plane of any Symmetry Wand
 * found in the acting player's inventory. The wand does not need to be held, only carried.
 */
@EventBusSubscriber(modid = SymmetryWandMod.MODID)
public final class SymmetryEventHandler {

    private SymmetryEventHandler() {
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        SymmetryPlane plane = findActivePlane(player);
        if (plane == null) {
            return;
        }

        BlockPos original = event.getPos();
        BlockState placedState = event.getPlacedBlock();
        Item item = placedState.getBlock().asItem();
        ItemStack placingStack = player.getMainHandItem().getItem() == item
                ? player.getMainHandItem()
                : player.getOffhandItem();
        // tracks units already promised to earlier mirrors in this same event, so multiple
        // mirrored copies can't each independently "claim" the same single spare unit
        int[] reservedFromPlacingStack = {0};

        for (SymmetryPlane.Mirror mirror : plane.distinctMirroredPositions(original)) {
            BlockPos mirrored = mirror.pos();
            if (!withinRange(plane, original, mirrored)) {
                continue;
            }

            if (!player.isCreative() && isOccupied(level.getBlockState(mirrored))) {
                warn(player, "The mirrored position is occupied - mirrored placement skipped.");
                continue;
            }

            BlockState mirroredState = mirrorFacing(placedState, mirror);

            if (!mirroredState.canSurvive(level, mirrored)) {
                warn(player, "The mirrored block has no support there - mirrored placement skipped.");
                continue;
            }

            if (Config.REQUIRE_RESOURCES_IN_SURVIVAL.get() && !player.isCreative()) {
                if (item == Items.AIR || !consumeItem(level, player, item, placingStack, reservedFromPlacingStack)) {
                    // leave the original placement untouched; only this mirrored copy is skipped
                    warn(player, "Not enough resources to mirror this block - mirrored placement skipped.");
                    continue;
                }
            }

            level.setBlockAndUpdate(mirrored, mirroredState);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player eventPlayer = event.getPlayer();
        if (!(eventPlayer instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        SymmetryPlane plane = findActivePlane(player);
        if (plane == null) {
            return;
        }

        BlockPos original = event.getPos();
        Block originalBlock = event.getState().getBlock();

        for (SymmetryPlane.Mirror mirror : plane.distinctMirroredPositions(original)) {
            BlockPos mirrored = mirror.pos();
            if (!withinRange(plane, original, mirrored)) {
                continue;
            }
            BlockState mirroredState = level.getBlockState(mirrored);
            if (mirroredState.isAir() || mirroredState.getBlock() != originalBlock) {
                continue;
            }

            if (!player.isCreative()) {
                BlockEntity blockEntity = mirroredState.hasBlockEntity() ? level.getBlockEntity(mirrored) : null;
                // drop at the block the player is actively mining, not the mirrored one
                Block.dropResources(mirroredState, level, original, blockEntity, player, player.getMainHandItem());
            }
            level.destroyBlock(mirrored, false, player);
        }
    }

    private static boolean withinRange(SymmetryPlane plane, BlockPos original, BlockPos mirrored) {
        int range = Config.MAX_RANGE.get();
        return plane.anchor().closerThan(original, range) && plane.anchor().closerThan(mirrored, range);
    }

    /** Air and liquids don't block a mirrored placement - only other solid blocks do. */
    private static boolean isOccupied(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    private static SymmetryPlane findActivePlane(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        SymmetryPlane plane = planeOf(mainHand);
        if (plane != null) {
            return plane;
        }
        ItemStack offHand = player.getOffhandItem();
        plane = planeOf(offHand);
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

    private static boolean consumeItem(ServerLevel level, ServerPlayer player, Item item, ItemStack placingStack, int[] reservedFromPlacingStack) {
        for (ItemStack stack : player.getInventory().items) {
            // NeoForge's onPlaceItemIntoWorld always resets the placing stack's count right after
            // this event, discarding any shrink applied to that exact object - so an immediate
            // shrink can only ever work on a genuinely separate stack
            if (stack == placingStack || stack.isEmpty() || stack.getItem() != item) {
                continue;
            }
            stack.shrink(1);
            // this slot isn't covered by the client sync that normally follows this interaction
            player.containerMenu.broadcastChanges();
            return true;
        }

        // no separate stack has stock; the placing stack can still pay if it has a spare unit
        // beyond the one vanilla is about to consume for the original block, and beyond any
        // units already promised to earlier mirrors of this same placement. That extra shrink
        // must be deferred until after NeoForge's own reset of this stack has already happened.
        if (!placingStack.isEmpty() && placingStack.getItem() == item
                && placingStack.getCount() - 1 - reservedFromPlacingStack[0] >= 1) {
            reservedFromPlacingStack[0]++;
            level.getServer().execute(() -> {
                if (!placingStack.isEmpty() && placingStack.getItem() == item) {
                    placingStack.shrink(1);
                    player.containerMenu.broadcastChanges();
                }
            });
            return true;
        }
        return false;
    }

    private static BlockState mirrorFacing(BlockState state, SymmetryPlane.Mirror mirror) {
        if (!Config.MIRROR_FACING.get()) {
            return state;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, transformFacing(state.getValue(BlockStateProperties.FACING), mirror));
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, transformFacing(state.getValue(BlockStateProperties.HORIZONTAL_FACING), mirror));
        }
        return state;
    }

    private static Direction transformFacing(Direction facing, SymmetryPlane.Mirror mirror) {
        if (mirror.axes().contains(facing.getAxis())) {
            facing = facing.getOpposite();
        }
        if (mirror.diagonal()) {
            facing = switch (facing) {
                case NORTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                default -> facing;
            };
        }
        if (mirror.antiDiagonal()) {
            facing = switch (facing) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                default -> facing;
            };
        }
        return facing;
    }

    private static void warn(ServerPlayer player, String message) {
        if (!Config.CHAT_FEEDBACK.get()) {
            return;
        }
        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }
}
