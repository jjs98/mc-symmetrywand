package com.symmetrywand.symmetry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A single infinite mirror plane, perpendicular to {@code axis}. The exact sum of the two
 * selected points' coordinates is kept for all three axes (not just the active one), so cycling
 * {@code axis} later can't lose the block-centered vs. between-blocks offset of the newly active
 * axis (each axis's offset depends independently on the two originally selected points).
 */
public record SymmetryPlane(Direction.Axis axis, int sumX, int sumY, int sumZ) {

    /** Places the plane centered on {@code pos} itself. */
    public static SymmetryPlane centeredAt(Direction.Axis axis, BlockPos pos) {
        return new SymmetryPlane(axis, 2 * pos.getX(), 2 * pos.getY(), 2 * pos.getZ());
    }

    /** Places the plane exactly between {@code a} and {@code b}. */
    public static SymmetryPlane between(Direction.Axis axis, BlockPos a, BlockPos b) {
        return new SymmetryPlane(axis, a.getX() + b.getX(), a.getY() + b.getY(), a.getZ() + b.getZ());
    }

    public BlockPos mirror(BlockPos pos) {
        int mirroredCoord = axisSum() - axisCoord(pos, axis);
        return switch (axis) {
            case X -> new BlockPos(mirroredCoord, pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), mirroredCoord, pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), mirroredCoord);
        };
    }

    /** Sum of the two selected points' coordinates along the active axis; see {@link #mirror}. */
    public int axisSum() {
        return switch (axis) {
            case X -> sumX;
            case Y -> sumY;
            case Z -> sumZ;
        };
    }

    /** The real plane coordinate along the active axis; may be a half-block (between two blocks). */
    public double planeCoord() {
        return (axisSum() + 1) / 2.0;
    }

    /** A reference block for range checks/display; not necessarily the precise plane position. */
    public BlockPos anchor() {
        return new BlockPos(Math.floorDiv(sumX, 2), Math.floorDiv(sumY, 2), Math.floorDiv(sumZ, 2));
    }

    public SymmetryPlane withAxis(Direction.Axis newAxis) {
        return new SymmetryPlane(newAxis, sumX, sumY, sumZ);
    }

    /** Cycles X -> Z -> Y -> X so shift-right-clicking changes the wand's orientation. */
    public static Direction.Axis cycle(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.Axis.Z;
            case Z -> Direction.Axis.Y;
            case Y -> Direction.Axis.X;
        };
    }

    private static int axisCoord(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }
}
