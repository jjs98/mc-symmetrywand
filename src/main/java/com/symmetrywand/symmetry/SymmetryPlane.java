package com.symmetrywand.symmetry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * One mirror plane per active axis in {@code axes}, all sharing the same two selected points.
 * The exact sum of those points' coordinates is kept for all three axes (not just the active
 * ones), so toggling axes later can't lose the block-centered vs. between-blocks offset of a
 * newly activated axis (each axis's offset depends independently on the two selected points).
 */
public record SymmetryPlane(AxisSelection axes, int sumX, int sumY, int sumZ) {

    /** Places the plane(s) centered on {@code pos} itself. */
    public static SymmetryPlane centeredAt(AxisSelection axes, BlockPos pos) {
        return new SymmetryPlane(axes, 2 * pos.getX(), 2 * pos.getY(), 2 * pos.getZ());
    }

    /** Places the plane(s) exactly between {@code a} and {@code b}. */
    public static SymmetryPlane between(AxisSelection axes, BlockPos a, BlockPos b) {
        return new SymmetryPlane(axes, a.getX() + b.getX(), a.getY() + b.getY(), a.getZ() + b.getZ());
    }

    /** A mirrored position together with exactly which axes were reflected to produce it. */
    public record Mirror(BlockPos pos, List<Direction.Axis> axes) {
    }

    /** Every mirrored position of {@code pos} across each non-empty combination of the active axes (excludes {@code pos} itself). */
    public List<Mirror> mirroredPositions(BlockPos pos) {
        List<Direction.Axis> activeAxes = activeAxes();
        int combinations = 1 << activeAxes.size();
        List<Mirror> result = new ArrayList<>(combinations - 1);
        for (int mask = 1; mask < combinations; mask++) {
            BlockPos mirrored = pos;
            List<Direction.Axis> applied = new ArrayList<>(activeAxes.size());
            for (int i = 0; i < activeAxes.size(); i++) {
                if ((mask & (1 << i)) != 0) {
                    Direction.Axis axis = activeAxes.get(i);
                    mirrored = mirrorAxis(mirrored, axis);
                    applied.add(axis);
                }
            }
            result.add(new Mirror(mirrored, applied));
        }
        return result;
    }

    /** Same as {@link #mirroredPositions}, but merges duplicate target positions (and their
     * axes) into one entry, and excludes {@code pos} itself. */
    public List<Mirror> distinctMirroredPositions(BlockPos pos) {
        LinkedHashMap<BlockPos, EnumSet<Direction.Axis>> merged = new LinkedHashMap<>();
        for (Mirror mirror : mirroredPositions(pos)) {
            if (mirror.pos().equals(pos)) {
                continue;
            }
            merged.computeIfAbsent(mirror.pos(), p -> EnumSet.noneOf(Direction.Axis.class)).addAll(mirror.axes());
        }
        List<Mirror> result = new ArrayList<>(merged.size());
        merged.forEach((p, axesSet) -> result.add(new Mirror(p, new ArrayList<>(axesSet))));
        return result;
    }

    public BlockPos mirrorAxis(BlockPos pos, Direction.Axis axis) {
        int mirroredCoord = axisSum(axis) - axisCoord(pos, axis);
        return switch (axis) {
            case X -> new BlockPos(mirroredCoord, pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), mirroredCoord, pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), mirroredCoord);
        };
    }

    public List<Direction.Axis> activeAxes() {
        List<Direction.Axis> result = new ArrayList<>(3);
        if (axes.mirrorX()) {
            result.add(Direction.Axis.X);
        }
        if (axes.mirrorY()) {
            result.add(Direction.Axis.Y);
        }
        if (axes.mirrorZ()) {
            result.add(Direction.Axis.Z);
        }
        return result;
    }

    /** Sum of the two selected points' coordinates along {@code axis}; see {@link #mirrorAxis}. */
    public int axisSum(Direction.Axis axis) {
        return switch (axis) {
            case X -> sumX;
            case Y -> sumY;
            case Z -> sumZ;
        };
    }

    /** The real plane coordinate along {@code axis}; may be a half-block (between two blocks). */
    public double planeCoord(Direction.Axis axis) {
        return (axisSum(axis) + 1) / 2.0;
    }

    /** A reference block for range checks/display; not necessarily the precise plane position. */
    public BlockPos anchor() {
        return new BlockPos(Math.floorDiv(sumX, 2), Math.floorDiv(sumY, 2), Math.floorDiv(sumZ, 2));
    }

    public SymmetryPlane withAxes(AxisSelection newAxes) {
        return new SymmetryPlane(newAxes, sumX, sumY, sumZ);
    }

    private static int axisCoord(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }
}
