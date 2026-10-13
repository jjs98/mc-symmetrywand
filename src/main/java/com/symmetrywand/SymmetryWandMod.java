package com.symmetrywand;

import com.symmetrywand.config.Config;
import com.symmetrywand.item.ModItems;
import com.symmetrywand.network.MirrorAxesHandler;
import com.symmetrywand.network.MirrorAxesPayload;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(SymmetryWandMod.MODID)
public class SymmetryWandMod {

    public static final String MODID = "symmetrywand";

    public SymmetryWandMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerPayloads);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.SYMMETRY_WAND);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(MirrorAxesPayload.TYPE, MirrorAxesPayload.STREAM_CODEC, MirrorAxesHandler::handle);
    }
}
