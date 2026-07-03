package com.ui_utils.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
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

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
    private HandledScreenMixin() {
        super(null);
    }

    // handleHotbarKeyPressed was renamed/removed in newer mappings (1.21.x). Use reflection to invoke if present.
    @Shadow
    protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);
    @Shadow
    @Nullable
    protected Slot focusedSlot;

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
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Unique
    private TextFieldWidget addressField;

    // called when creating a HandledScreen
    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (SharedVariables.enabled) {
            MainClient.createWidgets(mc, this);

            // create chat box
            this.addressField = new TextFieldWidget(this.textRenderer, 5, 245, 160, 20, Text.of("Chat ...")) {
                @Override
                public boolean keyPressed(KeyInput input) {
                    if (input.getKeycode() == GLFW.GLFW_KEY_ENTER) {
                        if (this.getText().equals("^toggleuiutils")) {
                            SharedVariables.enabled = !SharedVariables.enabled;
                            if (mc.player != null) {
                                mc.player.sendMessage(Text.of("UI-Utils is now " + (SharedVariables.enabled ? "enabled" : "disabled") + "."), false);

                            }
                            return false;
                        }

                        if (mc.getNetworkHandler() != null) {
                            if (this.getText().startsWith("/")) {
                                mc.getNetworkHandler().sendChatCommand(this.getText().replaceFirst(Pattern.quote("/"), ""));
                            } else {
                                mc.getNetworkHandler().sendChatMessage(this.getText());
                            }
                    } else {
                            MainClient.LOGGER.warn("Minecraft network handler (mc.getNetworkHandler()) was null while trying to send chat message from UI Utils.");
                        }

                        this.setText("");
                    }
                    return super.keyPressed(input);
                }
            };
            this.addressField.setText("");
            this.addressField.setMaxLength(256);

            this.addDrawableChild(this.addressField);
        }
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    public void keyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
        if (super.keyPressed(input)) {
            cir.setReturnValue(true);
        } else if (MainClient.mc.options.inventoryKey.matchesKey(input) && (this.addressField == null || !this.addressField.isSelected())) {
            // Crashes if address field does not exist (because of ui utils disabled, this is a temporary fix.)
            this.close();
            cir.setReturnValue(true);
        } else {
            // attempt to invoke the internal hotbar handler (name changed in mappings).
            this.invokeHandleHotbarKey(input.getKeycode(), input.scancode());
            if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
                if (mc.options.pickItemKey.matchesKey(input)) {
                    this.onMouseClick(this.focusedSlot, this.focusedSlot.id, 0, SlotActionType.CLONE);
                } else if (mc.options.dropKey.matchesKey(input)) {
                    this.onMouseClick(this.focusedSlot, this.focusedSlot.id, 0, SlotActionType.THROW);
                }
            }

            cir.setReturnValue(true);
        }
    }

    // inject at the end of the render method
    @Inject(at = @At("TAIL"), method = "render")
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // display sync id, revision, if ui utils is enabled
        // this hurts me physically to look at this in a render method :(
        // im too lazy to fix it tho :D
        if (SharedVariables.enabled) {
            MainClient.createText(mc, context, this.textRenderer);
        }
    }
}
