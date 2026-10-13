package com.symmetrywand.client;

import com.symmetrywand.symmetry.AxisSelection;
import com.symmetrywand.symmetry.WandData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Entry point for client-only wand interactions, kept out of the shared Item class. */
public final class ClientWandActions {

    private ClientWandActions() {
    }

    public static void openMirrorAxesScreen(ItemStack stack, InteractionHand hand) {
        AxisSelection current = WandData.getAxes(stack);
        Minecraft.getInstance().setScreen(new MirrorAxesScreen(current, hand == InteractionHand.MAIN_HAND));
    }
}
