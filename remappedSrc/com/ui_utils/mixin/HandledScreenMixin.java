package com.ui_utils.mixin;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ui_utils.MainClient;
import com.ui_utils.SharedVariables;

import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {
    private HandledScreenMixin() {
        super(null);
    }

    @Shadow
    protected abstract boolean handleHotbarKeyPressed(int keyCode, int scanCode);
    @Shadow
    protected abstract void onMouseClick(Slot slot, int slotId, int button, ClickType actionType);
    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Unique
    private static final Minecraft mc = Minecraft.getInstance();

    @Unique
    private EditBox addressField;

    // called when creating a HandledScreen
    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (SharedVariables.enabled) {
            MainClient.createWidgets(mc, this);

            // create chat box
            this.addressField = new EditBox(this.font, 5, 245, 160, 20, Component.nullToEmpty("Chat ...")) {
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
            this.addressField.setMaxLength(256);

            this.addRenderableWidget(this.addressField);
        }
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        } else if (MainClient.mc.options.keyInventory.matches(keyCode, scanCode) && (this.addressField == null || !this.addressField.isHoveredOrFocused())) {
            // Crashes if address field does not exist (because of ui utils disabled, this is a temporary fix.)
            this.onClose();
            cir.setReturnValue(true);
        } else {
            this.handleHotbarKeyPressed(keyCode, scanCode);
            if (this.focusedSlot != null && this.focusedSlot.hasItem()) {
                if (mc.options.keyPickItem.matches(keyCode, scanCode)) {
                    this.onMouseClick(this.focusedSlot, this.focusedSlot.index, 0, ClickType.CLONE);
                } else if (mc.options.keyDrop.matches(keyCode, scanCode)) {
                    this.onMouseClick(this.focusedSlot, this.focusedSlot.index, hasControlDown() ? 1 : 0, ClickType.THROW);
                }
            }

            cir.setReturnValue(true);
        }
    }

    // inject at the end of the render method
    @Inject(at = @At("TAIL"), method = "render")
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // display sync id, revision, if ui utils is enabled
        // this hurts me physically to look at this in a render method :(
        // im too lazy to fix it tho :D
        if (SharedVariables.enabled) {
            MainClient.createText(mc, context, this.font);
        }
    }
}
