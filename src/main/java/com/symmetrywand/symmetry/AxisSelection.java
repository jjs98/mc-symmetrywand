package com.symmetrywand.symmetry;

/** Which axes a symmetry wand mirrors across; a wand needs at least one true axis to be active. */
public record AxisSelection(boolean mirrorX, boolean mirrorY, boolean mirrorZ) {

    public static final AxisSelection DEFAULT = new AxisSelection(true, false, false);

    public boolean hasAny() {
        return mirrorX || mirrorY || mirrorZ;
    }
}
