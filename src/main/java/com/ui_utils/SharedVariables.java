package com.ui_utils;

import java.util.ArrayList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class SharedVariables {
    public static boolean sendUIPackets = true;
    public static boolean delayUIPackets = false;
    public static boolean shouldEditSign = true;

    public static ArrayList<Packet<?>> delayedUIPackets = new ArrayList<>();

    public static Screen storedScreen = null;
    public static AbstractContainerMenu storedScreenHandler = null;

    public static boolean enabled = true;
    public static boolean bypassResourcePack = false;
    public static boolean resourcePackForceDeny = false;
}
