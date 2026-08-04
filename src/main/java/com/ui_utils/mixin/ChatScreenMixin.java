package com.ui_utils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ui_utils.MainClient;
import com.ui_utils.SharedVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(at = @At("HEAD"), method = "handleChatInput", cancellable = true)
    public void sendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (chatText.equals("^toggleuiutils")) {
            SharedVariables.enabled = !SharedVariables.enabled;
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty("UI-Utils is now " + (SharedVariables.enabled ? "enabled" : "disabled") + "."));
            } else {
                MainClient.LOGGER.warn("Minecraft player was nulling while enabling / disabling UI Utils.");
            }
            Minecraft.getInstance().gui.hud.getChat().addRecentChat(chatText);
            Minecraft.getInstance().gui.setScreen(null);
            ci.cancel();
        }
    }
}
