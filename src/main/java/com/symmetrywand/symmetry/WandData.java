package com.symmetrywand.symmetry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import org.jetbrains.annotations.Nullable;

/**
 * Reads and writes a wand's {@link SymmetryPlane} to/from its persistent item data, so the plane
 * travels with the specific wand stack rather than with the player.
 */
public final class WandData {

    private static final String TAG_AXIS = "SymmetryAxis";
    private static final String TAG_SUM_X = "SymmetrySumX";
    private static final String TAG_SUM_Y = "SymmetrySumY";
    private static final String TAG_SUM_Z = "SymmetrySumZ";

    private static final String TAG_PENDING_AXIS = "PendingAxis";
    private static final String TAG_PENDING_X = "PendingX";
    private static final String TAG_PENDING_Y = "PendingY";
    private static final String TAG_PENDING_Z = "PendingZ";

    private static final String TAG_WAND_AXIS = "WandAxis";
    private static final Direction.Axis DEFAULT_AXIS = Direction.Axis.X;

    private WandData() {
    }

    @Nullable
    public static SymmetryPlane get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_AXIS)) {
            return null;
        }
        Direction.Axis axis = Direction.Axis.byName(tag.getString(TAG_AXIS));
        if (axis == null) {
            return null;
        }
        return new SymmetryPlane(axis, tag.getInt(TAG_SUM_X), tag.getInt(TAG_SUM_Y), tag.getInt(TAG_SUM_Z));
    }

    public static void set(ItemStack stack, SymmetryPlane plane) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            tag.putString(TAG_AXIS, plane.axis().getName());
            tag.putInt(TAG_SUM_X, plane.sumX());
            tag.putInt(TAG_SUM_Y, plane.sumY());
            tag.putInt(TAG_SUM_Z, plane.sumZ());
            return CustomData.of(tag);
        });
    }

    public static void clear(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }
        CompoundTag tag = data.copyTag();
        tag.remove(TAG_AXIS);
        tag.remove(TAG_SUM_X);
        tag.remove(TAG_SUM_Y);
        tag.remove(TAG_SUM_Z);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** The first point clicked while setting a plane between two blocks, awaiting a second click. */
    public record PendingPoint(Direction.Axis axis, BlockPos pos) {
    }

    @Nullable
    public static PendingPoint getPending(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_PENDING_AXIS)) {
            return null;
        }
        Direction.Axis axis = Direction.Axis.byName(tag.getString(TAG_PENDING_AXIS));
        if (axis == null) {
            return null;
        }
        BlockPos pos = new BlockPos(tag.getInt(TAG_PENDING_X), tag.getInt(TAG_PENDING_Y), tag.getInt(TAG_PENDING_Z));
        return new PendingPoint(axis, pos);
    }

    public static void setPending(ItemStack stack, Direction.Axis axis, BlockPos pos) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            tag.putString(TAG_PENDING_AXIS, axis.getName());
            tag.putInt(TAG_PENDING_X, pos.getX());
            tag.putInt(TAG_PENDING_Y, pos.getY());
            tag.putInt(TAG_PENDING_Z, pos.getZ());
            return CustomData.of(tag);
        });
    }

    public static void clearPending(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }
        CompoundTag tag = data.copyTag();
        tag.remove(TAG_PENDING_AXIS);
        tag.remove(TAG_PENDING_X);
        tag.remove(TAG_PENDING_Y);
        tag.remove(TAG_PENDING_Z);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** The wand's current orientation, changed only via shift-right-click; survives plane resets. */
    public static Direction.Axis getAxis(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            Direction.Axis axis = Direction.Axis.byName(data.copyTag().getString(TAG_WAND_AXIS));
            if (axis != null) {
                return axis;
            }
        }
        return DEFAULT_AXIS;
    }

    public static void setAxis(ItemStack stack, Direction.Axis axis) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            tag.putString(TAG_WAND_AXIS, axis.getName());
            return CustomData.of(tag);
        });
    }
}
