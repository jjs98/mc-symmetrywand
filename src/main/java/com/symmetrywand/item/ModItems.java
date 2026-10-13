package com.symmetrywand.item;

import com.symmetrywand.SymmetryWandMod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SymmetryWandMod.MODID);

    public static final DeferredItem<SymmetryWandItem> SYMMETRY_WAND = ITEMS.registerItem(
            "symmetry_wand",
            SymmetryWandItem::new,
            new Item.Properties().stacksTo(1)
    );

    private ModItems() {
    }
}
