package com.ui_utils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ui_utils.SharedVariables;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

@Mixin(JoinMultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen {
    private MultiplayerScreenMixin() {
        super(null);
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (SharedVariables.enabled) {
            // Create "Bypass Resource Pack" option
            this.addRenderableWidget(Button.builder(Component.nullToEmpty("Bypass Resource Pack: " + (SharedVariables.bypassResourcePack ? "ON" : "OFF")), (button) -> {
                SharedVariables.bypassResourcePack = !SharedVariables.bypassResourcePack;
                button.setMessage(Component.nullToEmpty("Bypass Resource Pack: " + (SharedVariables.bypassResourcePack ? "ON" : "OFF")));
            }).width(160).pos(this.width - 170, this.height - 50).build());

            // Create "Force Deny" option
            this.addRenderableWidget(Button.builder(Component.nullToEmpty("Force Deny: " + (SharedVariables.resourcePackForceDeny ? "ON" : "OFF")), (button) -> {
                SharedVariables.resourcePackForceDeny = !SharedVariables.resourcePackForceDeny;
                button.setMessage(Component.nullToEmpty("Force Deny: " + (SharedVariables.resourcePackForceDeny ? "ON" : "OFF")));
            }).width(160).pos(this.width - 170, this.height - 25).build());
        }
    }
}
