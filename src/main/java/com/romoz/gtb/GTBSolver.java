package com.romoz.gtb;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import com.romoz.gtb.ui.GTBOverlayScreen;
import com.romoz.gtb.ui.PatternState;

public class GTBSolver implements ClientModInitializer {

    private static KeyBinding openOverlayKey;

    @Override
    public void onInitializeClient() {
        openOverlayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.gtbsolver.open_overlay", // lang key
                GLFW.GLFW_KEY_GRAVE_ACCENT,   // `
                "key.categories.gtbsolver"    // category
        ));

        // Тик-обработчик для клавиши
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openOverlayKey.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.currentScreen == null) {
                    mc.setScreen(new GTBOverlayScreen(PatternState.get()));
                }
            }
        });
    }
}
