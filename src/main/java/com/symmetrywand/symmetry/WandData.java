package com.symmetrywand.symmetry;

import net.minecraft.core.BlockPos;
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

    private static final String TAG_MIRROR_X = "SymmetryMirrorX";
    private static final String TAG_MIRROR_Y = "SymmetryMirrorY";
    private static final String TAG_MIRROR_Z = "SymmetryMirrorZ";
    private static final String TAG_OCTAGONAL = "SymmetryOctagonal";
    private static final String TAG_SUM_X = "SymmetrySumX";
    private static final String TAG_SUM_Y = "SymmetrySumY";
    private static final String TAG_SUM_Z = "SymmetrySumZ";

    private static final String TAG_PENDING = "Pending";
    private static final String TAG_PENDING_MIRROR_X = "PendingMirrorX";
    private static final String TAG_PENDING_MIRROR_Y = "PendingMirrorY";
    private static final String TAG_PENDING_MIRROR_Z = "PendingMirrorZ";
    private static final String TAG_PENDING_OCTAGONAL = "PendingOctagonal";
    private static final String TAG_PENDING_X = "PendingX";
    private static final String TAG_PENDING_Y = "PendingY";
    private static final String TAG_PENDING_Z = "PendingZ";

    private static final String TAG_WAND_MIRROR_X = "WandMirrorX";
    private static final String TAG_WAND_MIRROR_Y = "WandMirrorY";
    private static final String TAG_WAND_MIRROR_Z = "WandMirrorZ";
    private static final String TAG_WAND_OCTAGONAL = "WandOctagonal";

    private WandData() {
    }

    @Nullable
    public static SymmetryPlane get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_SUM_X)) {
            return null;
        }
        AxisSelection axes = readAxes(tag, TAG_MIRROR_X, TAG_MIRROR_Y, TAG_MIRROR_Z, TAG_OCTAGONAL);
        return new SymmetryPlane(axes, tag.getInt(TAG_SUM_X), tag.getInt(TAG_SUM_Y), tag.getInt(TAG_SUM_Z));
    }

    public static void set(ItemStack stack, SymmetryPlane plane) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            writeAxes(tag, TAG_MIRROR_X, TAG_MIRROR_Y, TAG_MIRROR_Z, TAG_OCTAGONAL, plane.axes());
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
        tag.remove(TAG_MIRROR_X);
        tag.remove(TAG_MIRROR_Y);
        tag.remove(TAG_MIRROR_Z);
        tag.remove(TAG_OCTAGONAL);
        tag.remove(TAG_SUM_X);
        tag.remove(TAG_SUM_Y);
        tag.remove(TAG_SUM_Z);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** The first point clicked while setting a plane between two blocks, awaiting a second click. */
    public record PendingPoint(AxisSelection axes, BlockPos pos) {
    }

    @Nullable
    public static PendingPoint getPending(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_PENDING)) {
            return null;
        }
        AxisSelection axes = readAxes(tag, TAG_PENDING_MIRROR_X, TAG_PENDING_MIRROR_Y, TAG_PENDING_MIRROR_Z, TAG_PENDING_OCTAGONAL);
        BlockPos pos = new BlockPos(tag.getInt(TAG_PENDING_X), tag.getInt(TAG_PENDING_Y), tag.getInt(TAG_PENDING_Z));
        return new PendingPoint(axes, pos);
    }

    public static void setPending(ItemStack stack, AxisSelection axes, BlockPos pos) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            tag.putBoolean(TAG_PENDING, true);
            writeAxes(tag, TAG_PENDING_MIRROR_X, TAG_PENDING_MIRROR_Y, TAG_PENDING_MIRROR_Z, TAG_PENDING_OCTAGONAL, axes);
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
        tag.remove(TAG_PENDING);
        tag.remove(TAG_PENDING_MIRROR_X);
        tag.remove(TAG_PENDING_MIRROR_Y);
        tag.remove(TAG_PENDING_MIRROR_Z);
        tag.remove(TAG_PENDING_OCTAGONAL);
        tag.remove(TAG_PENDING_X);
        tag.remove(TAG_PENDING_Y);
        tag.remove(TAG_PENDING_Z);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** The wand's current axis selection, changed only via the mirror-axes screen; survives plane resets. */
    public static AxisSelection getAxes(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return AxisSelection.DEFAULT;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(TAG_WAND_MIRROR_X) && !tag.contains(TAG_WAND_MIRROR_Y) && !tag.contains(TAG_WAND_MIRROR_Z)
                && !tag.contains(TAG_WAND_OCTAGONAL)) {
            return AxisSelection.DEFAULT;
        }
        return readAxes(tag, TAG_WAND_MIRROR_X, TAG_WAND_MIRROR_Y, TAG_WAND_MIRROR_Z, TAG_WAND_OCTAGONAL);
    }

    public static void setAxes(ItemStack stack, AxisSelection axes) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
            CompoundTag tag = data.copyTag();
            writeAxes(tag, TAG_WAND_MIRROR_X, TAG_WAND_MIRROR_Y, TAG_WAND_MIRROR_Z, TAG_WAND_OCTAGONAL, axes);
            return CustomData.of(tag);
        });
    }

    private static AxisSelection readAxes(CompoundTag tag, String xTag, String yTag, String zTag, String octagonalTag) {
        return new AxisSelection(tag.getBoolean(xTag), tag.getBoolean(yTag), tag.getBoolean(zTag), tag.getBoolean(octagonalTag));
    }

    private static void writeAxes(CompoundTag tag, String xTag, String yTag, String zTag, String octagonalTag, AxisSelection axes) {
        tag.putBoolean(xTag, axes.mirrorX());
        tag.putBoolean(yTag, axes.mirrorY());
        tag.putBoolean(zTag, axes.mirrorZ());
        tag.putBoolean(octagonalTag, axes.octagonal());
    }
}
