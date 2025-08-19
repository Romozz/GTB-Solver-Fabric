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
    // Обработка клика по слову с учетом задержки и отправка слова от имени игрока
    public static void handleWordClick(String word, boolean withArrows) {
        long currentTime = System.currentTimeMillis();
        long lastClickTime = lastClickTimes.getOrDefault(word, 0L);

        if (currentTime - lastClickTime < CLICK_DELAY) {
            // Если прошло менее 3 секунд с последнего клика
            sendFeedback("You must wait 4 seconds before clicking this word again.");
        } else {
            // Обновляем время последнего клика
            lastClickTimes.put(word, currentTime);

            // Отправляем сообщение с выбранным словом в чат и на сервер
            sendWordToChat(word, withArrows);
        }
    }

    // Отправка выбранного слова в чат (с учетом стрелочек)
    public static void sendWordToChat(String word, boolean withArrows) {
        if (MinecraftClient.getInstance().player != null) {
            String formattedWord = withArrows ? "===== " + word + " =====" : word;
            // Отправляем сообщение от имени игрока
            MinecraftClient.getInstance().player.networkHandler.sendChatMessage(formattedWord);
        }
    }
    }
}

