package com.symmetrywand.network;

import com.symmetrywand.config.Config;
import com.symmetrywand.item.SymmetryWandItem;
import com.symmetrywand.symmetry.AxisSelection;
import com.symmetrywand.symmetry.SymmetryPlane;
import com.symmetrywand.symmetry.WandData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Applies an axis selection sent by the client's mirror-axes screen to the wand that opened it. */
public final class MirrorAxesHandler {

    private MirrorAxesHandler() {
    }

    public static void handle(MirrorAxesPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        AxisSelection axes = new AxisSelection(payload.mirrorX(), payload.mirrorY(), payload.mirrorZ());
        if (!axes.hasAny()) {
            return;
        }

        ItemStack stack = player.getItemInHand(payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (!(stack.getItem() instanceof SymmetryWandItem)) {
            return;
        }

        WandData.setAxes(stack, axes);
        SymmetryPlane plane = WandData.get(stack);
        if (plane != null) {
            WandData.set(stack, plane.withAxes(axes));
        }

        if (Config.CHAT_FEEDBACK.get()) {
            player.sendSystemMessage(Component.literal("Symmetry wand axes updated.").withStyle(ChatFormatting.GREEN));
        }
    }
}
