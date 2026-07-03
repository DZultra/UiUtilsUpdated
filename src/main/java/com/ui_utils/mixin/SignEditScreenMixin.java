package com.ui_utils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ui_utils.SharedVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.network.chat.Component;

@Mixin(SignEditScreen.class)
public class SignEditScreenMixin extends Screen {
    protected SignEditScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private static final Minecraft mc = Minecraft.getInstance();

    // called when any sign edit screen is created
    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {

        // register "close without packet" button for SignEditScreen if ui utils is enabled
        if (SharedVariables.enabled) {
            addRenderableWidget(Button.builder(Component.nullToEmpty("Close without packet"), (button) -> {
                // disables sign editing and closes the current gui without sending a packet
                SharedVariables.shouldEditSign = false;
                mc.setScreen(null);
            }).width(115).pos(5, 5).build());
            addRenderableWidget(Button.builder(Component.nullToEmpty("Disconnect"), (button) -> {
                if (mc.getConnection() != null) {
                    mc.getConnection().getConnection().disconnect(Component.nullToEmpty("Disconnecting (UI-UTILS)"));
                }
            }).width(115).pos(5, 35).build());
        }
    }
}
