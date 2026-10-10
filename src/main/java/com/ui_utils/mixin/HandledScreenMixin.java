package com.ui_utils.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.ui_utils.MainClient;
import com.ui_utils.SharedVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Pattern;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {
    private HandledScreenMixin() {
        super(null);
    }

    // handleHotbarKeyPressed was renamed/removed in newer mappings (1.21.x). Use reflection to invoke if present.
    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int button, ContainerInput actionType);
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Unique
    private java.lang.reflect.Method uiUtils_handleHotbarMethod = null;

    @Unique
    private boolean invokeHandleHotbarKey(int keyCode) {
        if (this.uiUtils_handleHotbarMethod == null) {
            String[] names = new String[]{
                    "checkHotbarKeyPressed",   // <-- ADD THIS (Mojang mappings 1.21+ / 26.2)
                    "handleHotbarKeyPressed",
                    "handleHotbarKeyPress",
                    "handleHotbarKey",
                    "onHotbarKey"
            };
            Class<?> cls = this.getClass();
            for (String name : names) {
                try {
                    // Use getMethod to search inherited methods too
                    java.lang.reflect.Method m = cls.getMethod(name, int.class, int.class);
                    m.setAccessible(true);
                    this.uiUtils_handleHotbarMethod = m;
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
        }

        if (this.uiUtils_handleHotbarMethod != null) {
            try {
                Object ret = this.uiUtils_handleHotbarMethod.invoke(this, keyCode);
                if (ret instanceof Boolean) return (Boolean) ret;
            } catch (ReflectiveOperationException ignored) {}
        }
        return false;
    }

    @Unique
    private static final Minecraft mc = Minecraft.getInstance();

    @Unique
    private EditBox addressField;

    // called when creating a HandledScreen
    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (SharedVariables.enabled) {
            MainClient.createWidgets(mc, this);
            this.addressField = new EditBox(this.font, 5, 245, 160, 20, Component.nullToEmpty("Chat ...")) {
                @Override
                public boolean keyPressed(@NonNull KeyEvent input) {
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
                        }
                        this.setValue("");
                    }
                    return super.keyPressed(input);
                }
            };
            this.addressField.setValue("");
            this.addressField.setMaxLength(256);
            this.addRenderableWidget(this.addressField);
        }
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    public void keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        // If our chat box is focused, let it handle everything and block vanilla
        if (this.addressField != null && this.addressField.isFocused()) {
            if (this.addressField.keyPressed(input)) {
                cir.setReturnValue(true);
                return;
            }
            // Still cancel so vanilla doesn't close inventory on 'E' while typing
            cir.setReturnValue(true);
            return;
        }

        if (super.keyPressed(input)) {
            cir.setReturnValue(true);
        } else if (MainClient.mc.options.keyInventory.matches(input)
                && (this.addressField == null || !this.addressField.isHoveredOrFocused())) {
            this.onClose();
            cir.setReturnValue(true);
        } else {
            if (this.invokeHandleHotbarKey(input.input())) {
                cir.setReturnValue(true);
                return;
            }
            if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                if (mc.options.keyPickItem.matches(input)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ContainerInput.CLONE);
                    cir.setReturnValue(true);
                    return;
                } else if (mc.options.keyDrop.matches(input)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ContainerInput.THROW);
                    cir.setReturnValue(true);
                    return;
                }
            }
            // Do NOT cancel here — let vanilla handle number keys and anything else
        }
    }

    // inject at the end of the render method
    @Inject(at = @At("TAIL"), method = "extractRenderState")
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // display sync id, revision, if ui utils is enabled
        // this hurts me physically to look at this in a render method :(
        // im too lazy to fix it tho :D
        if (SharedVariables.enabled) {
            MainClient.createText(mc, context, this.font);
        }
    }
}
