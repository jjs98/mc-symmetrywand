package com.symmetrywand.symmetry;

/**
 * Which mirroring a symmetry wand applies; a wand needs at least one active option.
 * {@code octagonal} mirrors across the two diagonal planes through the anchor in addition to
 * whatever else is selected, giving 8-way symmetry around the X/Z center when combined with
 * mirrorX and mirrorZ. mirrorY stays independent and can be combined with either mode.
 */
public record AxisSelection(boolean mirrorX, boolean mirrorY, boolean mirrorZ, boolean octagonal) {

    public static final AxisSelection DEFAULT = new AxisSelection(true, false, false, false);

    public boolean hasAny() {
        return mirrorX || mirrorY || mirrorZ || octagonal;
    }
}
