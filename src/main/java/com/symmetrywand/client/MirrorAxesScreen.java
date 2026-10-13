package com.symmetrywand.client;

import com.symmetrywand.network.MirrorAxesPayload;
import com.symmetrywand.symmetry.AxisSelection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Lets the player pick which axes a symmetry wand mirrors across (at least one is required). */
public class MirrorAxesScreen extends Screen {

    private final boolean mainHand;
    private boolean mirrorX;
    private boolean mirrorY;
    private boolean mirrorZ;
    private boolean octagonal;
    private Button saveButton;

    public MirrorAxesScreen(AxisSelection initial, boolean mainHand) {
        super(Component.literal("Symmetry Wand - Mirror Axes"));
        this.mirrorX = initial.mirrorX();
        this.mirrorY = initial.mirrorY();
        this.mirrorZ = initial.mirrorZ();
        this.octagonal = initial.octagonal();
        this.mainHand = mainHand;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = this.height / 2 - 60;

        addRenderableWidget(Checkbox.builder(Component.literal("Mirror X"), this.font)
                .pos(centerX - 50, top)
                .selected(mirrorX)
                .onValueChange((checkbox, value) -> onAxisChanged(() -> mirrorX = value))
                .build());

        addRenderableWidget(Checkbox.builder(Component.literal("Mirror Y"), this.font)
                .pos(centerX - 50, top + 24)
                .selected(mirrorY)
                .onValueChange((checkbox, value) -> onAxisChanged(() -> mirrorY = value))
                .build());

        addRenderableWidget(Checkbox.builder(Component.literal("Mirror Z"), this.font)
                .pos(centerX - 50, top + 48)
                .selected(mirrorZ)
                .onValueChange((checkbox, value) -> onAxisChanged(() -> mirrorZ = value))
                .build());

        addRenderableWidget(Checkbox.builder(Component.literal("Octagonal"), this.font)
                .pos(centerX - 50, top + 72)
                .selected(octagonal)
                .onValueChange((checkbox, value) -> onAxisChanged(() -> octagonal = value))
                .build());

        saveButton = Button.builder(Component.literal("Save"), button -> save())
                .pos(centerX - 50, top + 104)
                .size(100, 20)
                .build();
        saveButton.active = hasAnyAxis();
        addRenderableWidget(saveButton);
    }

    private void onAxisChanged(Runnable apply) {
        apply.run();
        saveButton.active = hasAnyAxis();
    }

    private boolean hasAnyAxis() {
        return mirrorX || mirrorY || mirrorZ || octagonal;
    }

    private void save() {
        if (!hasAnyAxis()) {
            return;
        }
        PacketDistributor.sendToServer(new MirrorAxesPayload(mirrorX, mirrorY, mirrorZ, octagonal, mainHand));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 80, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
