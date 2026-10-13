package com.symmetrywand.symmetry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
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

    /** A mirrored position together with exactly which reflections were applied to produce it. */
    public record Mirror(BlockPos pos, List<Direction.Axis> axes, boolean diagonal, boolean antiDiagonal) {
    }

    /** Every mirrored position of {@code pos} across each non-empty combination of the active axes
     * and, if {@code axes.octagonal()}, the two diagonal reflections through the anchor (excludes {@code pos} itself). */
    public List<Mirror> mirroredPositions(BlockPos pos) {
        List<Direction.Axis> activeAxes = activeAxes();
        boolean octagonal = axes.octagonal();
        int diagonalBit = activeAxes.size();
        int antiDiagonalBit = activeAxes.size() + 1;
        int generatorCount = activeAxes.size() + (octagonal ? 2 : 0);
        int combinations = 1 << generatorCount;
        List<Mirror> result = new ArrayList<>(Math.max(0, combinations - 1));
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
            boolean diagonal = octagonal && (mask & (1 << diagonalBit)) != 0;
            boolean antiDiagonal = octagonal && (mask & (1 << antiDiagonalBit)) != 0;
            if (diagonal) {
                mirrored = mirrorDiagonal(mirrored);
            }
            if (antiDiagonal) {
                mirrored = mirrorAntiDiagonal(mirrored);
            }
            result.add(new Mirror(mirrored, applied, diagonal, antiDiagonal));
        }
        return result;
    }

    /** Same as {@link #mirroredPositions}, but keeps only the first combination reaching each
     * target position and excludes {@code pos} itself. Different combinations that land on the
     * same position are always algebraically the same overall transform (just expressed with
     * different generators), so their axes/diagonal/antiDiagonal flags must NOT be unioned
     * together - that would describe a different (usually wrong) transform, e.g. applying both
     * diagonal and antiDiagonal when only one or the other actually happened. */
    public List<Mirror> distinctMirroredPositions(BlockPos pos) {
        LinkedHashMap<BlockPos, Mirror> firstByPosition = new LinkedHashMap<>();
        for (Mirror mirror : mirroredPositions(pos)) {
            if (mirror.pos().equals(pos)) {
                continue;
            }
            firstByPosition.putIfAbsent(mirror.pos(), mirror);
        }
        return new ArrayList<>(firstByPosition.values());
    }

    public BlockPos mirrorAxis(BlockPos pos, Direction.Axis axis) {
        int mirroredCoord = axisSum(axis) - axisCoord(pos, axis);
        return switch (axis) {
            case X -> new BlockPos(mirroredCoord, pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), mirroredCoord, pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), mirroredCoord);
        };
    }

    /** Octagonal's diagonal mirrors only land exactly on blocks when the X and Z centers are
     * both block-centered or both boundary-centered (same parity); otherwise they'd be off by
     * half a block (see mirrorDiagonal/mirrorAntiDiagonal). Irrelevant if octagonal is off. */
    public boolean isOctagonalAligned() {
        return !axes.octagonal() || (sumX - sumZ) % 2 == 0;
    }

    /** Reflects across the diagonal line x - z = const (swaps X/Z offsets); const is derived from
     * sumX and sumZ the same way axisSum drives axis mirrors, so this can sit between two
     * diagonally-adjacent blocks too, not just centered on one. When sumX and sumZ have different
     * parity there's no exact integer solution, so the nearest one is used (floorDiv). */
    public BlockPos mirrorDiagonal(BlockPos pos) {
        int diagonalConstant = Math.floorDiv(sumX - sumZ, 2);
        return new BlockPos(diagonalConstant + pos.getZ(), pos.getY(), pos.getX() - diagonalConstant);
    }

    /** Reflects across the diagonal line x - z = -(z) i.e. x + z = const; see {@link #mirrorDiagonal}. */
    public BlockPos mirrorAntiDiagonal(BlockPos pos) {
        int antiDiagonalConstant = Math.floorDiv(sumX + sumZ, 2);
        return new BlockPos(antiDiagonalConstant - pos.getZ(), pos.getY(), antiDiagonalConstant - pos.getX());
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
