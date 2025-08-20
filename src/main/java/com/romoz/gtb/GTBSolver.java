package com.romoz.gtb;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import com.romoz.gtb.ui.GTBOverlayScreen;
import com.romoz.gtb.ui.PatternState;

import java.util.HashMap;
import java.util.Map;

public class GTBSolver implements ClientModInitializer {

    private static KeyBinding openOverlayKey;

    // анти-спам на клики по словам
    private static final Map<String, Long> lastClickTimes = new HashMap<>();
    private static final long CLICK_DELAY = 4000L; // мс

    @Override
    public void onInitializeClient() {
        PatternState.get().load();
        openOverlayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.gtbsolver.open_overlay",   // lang key
                GLFW.GLFW_KEY_GRAVE_ACCENT,     // `
                "key.categories.gtbsolver"      // category
        ));

        // тик-обработчик для клавиши открытия оверлея
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openOverlayKey.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.currentScreen == null) {
                    mc.setScreen(new GTBOverlayScreen());
                }
            }
        });
    } // <-- ВАЖНО: метод onInitializeClient ЗАКРЫТ здесь

    /** Обработка клика по слову с задержкой и отправкой в чат */
    public static void handleWordClick(String word, boolean withArrows) {
        long now = System.currentTimeMillis();
        long last = lastClickTimes.getOrDefault(word, 0L);

        if (now - last < CLICK_DELAY) {
            sendFeedback("You must wait 4 seconds before clicking this word again.");
            return;
        }

        lastClickTimes.put(word, now);
        sendWordToChat(word, withArrows);
    }

    /** Отправка выбранного слова в общий чат (на сервер) */
    public static void sendWordToChat(String word, boolean withArrows) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.networkHandler != null) {
            String msg = withArrows ? ("===== " + word + " =====") : word;
            mc.player.networkHandler.sendChatMessage(msg);
        }
    }

    /** Локальный фидбек в чат игрока (не на сервер) */
    public static void sendFeedback(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.inGameHud.getChatHud().addMessage(Text.literal(message));
        }
    }
}


