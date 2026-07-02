package com.ui_utils.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ui_utils.MainClient;
import com.ui_utils.SharedVariables;
import com.ui_utils.mixin.accessor.ScreenAccessor;

import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("all")
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow
    public abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addDrawableChild(T drawableElement);

    private static final Minecraft mc = Minecraft.getInstance();

    private EditBox addressField;
    private boolean initialized = false;

    // inject at the end of the render method (if instanceof LecternScreen)
    @Inject(at = @At("TAIL"), method = "init(Lnet/minecraft/client/MinecraftClient;II)V")
    public void init(Minecraft client, int width, int height, CallbackInfo ci) {
        // check if the current gui is a lectern gui and if ui-utils is enabled
        if (mc.screen instanceof LecternScreen screen && SharedVariables.enabled) {
            // setup widgets
            if (/*!this.initialized*/ true) { // bro why did you do this cxg :skull:
                // check if the current gui is a lectern gui and ui-utils is enabled
                // if you do not message me about this @coderx-gamer you are not reading my commits
                // why would you read them anyway tbh
                // ill clean this up later if you dont fix it

                Font textRenderer = ((ScreenAccessor) this).getTextRenderer();
                MainClient.createWidgets(mc, screen);

                // create chat box
                this.addressField = new EditBox(textRenderer, 5, 245, 160, 20, Component.nullToEmpty("Chat ...")) {
                    @Override
                    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                        if (keyCode == GLFW.GLFW_KEY_ENTER) {
                            if (this.getValue().equals("^toggleuiutils")) {
                                SharedVariables.enabled = !SharedVariables.enabled;
                                if (mc.player != null) {
                                    mc.player.displayClientMessage(Component.nullToEmpty("UI-Utils is now " + (SharedVariables.enabled ? "enabled" : "disabled") + "."), false);
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
                        return super.keyPressed(keyCode, scanCode, modifiers);
                    }
                };
                this.addressField.setValue("");
                this.addressField.setMaxLength(255);

                this.addDrawableChild(this.addressField);
                this.initialized = true;
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // display sync id, revision, if ui utils is enabled
        if (SharedVariables.enabled && mc.player != null && mc.screen instanceof LecternScreen) {
            MainClient.createText(mc, context, ((ScreenAccessor) this).getTextRenderer());
        }
    }
}
