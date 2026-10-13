package com.symmetrywand.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_RANGE = BUILDER
            .comment("Maximum distance (in blocks) a symmetry plane may be anchored from the player, " +
                    "and the maximum distance mirrored block actions will reach from that anchor.")
            .defineInRange("maxRange", 32, 1, 256);

    public static final ModConfigSpec.IntValue MAX_SELECTION_DISTANCE = BUILDER
            .comment("Maximum distance (in blocks) allowed between the two points clicked when setting " +
                    "a symmetry plane between them. If exceeded, the selection is canceled.")
            .defineInRange("maxSelectionDistance", 16, 1, 256);

    public static final ModConfigSpec.BooleanValue MIRROR_FACING = BUILDER
            .comment("Whether directional block states (e.g. furnaces, stairs, dispensers) have their " +
                    "facing flipped when mirrored across the symmetry plane.")
            .define("mirrorFacing", true);

    public static final ModConfigSpec.BooleanValue REQUIRE_RESOURCES_IN_SURVIVAL = BUILDER
            .comment("Whether mirrored block placements must be paid for with an extra matching item " +
                    "from the player's inventory while in Survival mode. If the player lacks the resources, " +
                    "the placement is canceled.")
            .define("requireResourcesInSurvival", true);

    public static final ModConfigSpec.BooleanValue CHAT_FEEDBACK = BUILDER
            .comment("Whether the Symmetry Wand reports status, warnings and errors in the chat.")
            .define("chatFeedback", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
