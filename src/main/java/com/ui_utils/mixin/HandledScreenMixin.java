package com.ui_utils.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.ContainerInput;
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

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
    private boolean invokeHandleHotbarKey(int keyCode, int scanCode) {
        if (this.uiUtils_handleHotbarMethod == null) {
            // try several likely names, then fallback to any method taking (int,int) and returning boolean
            String[] names = new String[]{"handleHotbarKeyPressed", "handleHotbarKeyPress", "handleHotbarKey", "onHotbarKey"};
            Class<?> cls = this.getClass();
            for (String name : names) {
                try {
                    java.lang.reflect.Method m = cls.getDeclaredMethod(name, int.class, int.class);
                    m.setAccessible(true);
                    this.uiUtils_handleHotbarMethod = m;
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (this.uiUtils_handleHotbarMethod == null) {
                for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                    if (m.getReturnType() == boolean.class) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 2 && params[0] == int.class && params[1] == int.class) {
                            m.setAccessible(true);
                            this.uiUtils_handleHotbarMethod = m;
                            break;
                        }
                    }
                }
            }
        }

        if (this.uiUtils_handleHotbarMethod != null) {
            try {
                Object ret = this.uiUtils_handleHotbarMethod.invoke(this, keyCode, scanCode);
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

            // create chat box
            this.addressField = new EditBox(this.font, 5, 245, 160, 20, Component.nullToEmpty("Chat ...")) {
                @Override
                public boolean keyPressed(KeyEvent input) {
                    if (input.input() == GLFW.GLFW_KEY_ENTER) {
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
            this.addressField.setValue("");
            this.addressField.setMaxLength(256);

            this.addRenderableWidget(this.addressField);
        }
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    public void keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
        if (super.keyPressed(input)) {
            cir.setReturnValue(true);
        } else if (MainClient.mc.options.keyInventory.matches(input) && (this.addressField == null || !this.addressField.isHoveredOrFocused())) {
            // Crashes if address field does not exist (because of ui utils disabled, this is a temporary fix.)
            this.onClose();
            cir.setReturnValue(true);
        } else {
            // attempt to invoke the internal hotbar handler (name changed in mappings).
            this.invokeHandleHotbarKey(input.input(), input.scancode());
            if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                if (mc.options.keyPickItem.matches(input)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ContainerInput.CLONE);
                } else if (mc.options.keyDrop.matches(input)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ContainerInput.THROW);
                }
            }

            cir.setReturnValue(true);
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
