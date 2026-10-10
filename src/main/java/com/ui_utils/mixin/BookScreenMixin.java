package com.ui_utils.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ui_utils.MainClient;
import com.ui_utils.SharedVariables;

import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

@Mixin(BookViewScreen.class)
public class BookScreenMixin extends Screen {
    protected BookScreenMixin(Component title) {
        super(title);
    }
    @Unique
    private static final Minecraft mc = Minecraft.getInstance();

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (SharedVariables.enabled) {
            MainClient.createWidgets(mc, this);

            // create chat box
            EditBox addressField = new EditBox(font, 5, 245, 160, 20, Component.nullToEmpty("Chat ...")) {
                @Override
                public boolean keyPressed(KeyEvent input) {
                    if (input.input() == InputConstants.KEY_RETURN) {
                        if (this.getValue().equals("^toggleuiutils")) {
                            SharedVariables.enabled = !SharedVariables.enabled;
                            if (mc.player != null) {
                                mc.player.sendSystemMessage(Component.nullToEmpty("UI-Utils is now " + (SharedVariables.enabled ? "enabled" : "disabled") + "."));
                            }
                            return false;
                        }

                        if (mc.getConnection() != null) {
                            if (this.getValue().startsWith("/")) {
                                mc.getConnection().sendCommand(this.getValue().replaceFirst(Pattern.quote("/"), ""));
                            } else {
                                mc.getConnection().sendChat(this.getValue());
                            }
                        } else {
                            MainClient.LOGGER.warn("Minecraft network handler (mc.getNetworkHandler()) was null while trying to send chat message from UI Utils.");
                        }

                        this.setValue("");
                    }
                    return super.keyPressed(input);
                }
            };
            addressField.setValue("");
            addressField.setMaxLength(255);

            this.addRenderableWidget(addressField);
        }
    }
}
