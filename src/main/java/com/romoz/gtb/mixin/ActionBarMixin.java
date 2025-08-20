package com.romoz.gtb.mixin;

import com.romoz.gtb.GTBHelper;
import com.romoz.gtb.ui.PatternState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(InGameHud.class)
public abstract class ActionBarMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("TAIL"))
    private void gtb$onOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        if (message == null) return;
        String raw = message.getString();
        if (raw == null || raw.isEmpty()) return;

        // 1) Парсим длину, если она явно указана
        Matcher lenM = Pattern.compile("(?:\\b(?:len(?:gth)?|letters?)\\s*[:=-]?\\s*)(\\d{1,2})",
                Pattern.CASE_INSENSITIVE).matcher(raw);
        Integer parsedLen = null;
        if (lenM.find()) parsedLen = Integer.parseInt(lenM.group(1));

        // 2) Ищем «маску» из букв и подчёркиваний
        Matcher maskM = Pattern.compile("([_A-Za-z\\s]{2,})").matcher(raw);
        String best = null;
        while (maskM.find()) {
            String s = maskM.group(1).trim();
            if (s.chars().anyMatch(ch -> ch == '_' || Character.isLetter(ch))) {
                if (best == null || s.length() > best.length()) best = s;
            }
        }
        if (best == null && parsedLen == null) return;

        String normalized = null;
        if (best != null) {
            normalized = best.replaceAll("\\s+", "").replaceAll("[^A-Za-z_]", "");
        }

        // >>> ключевой фикс: делаем переменные "effectively final" для лямбды
        final String normalizedF = normalized;
        final Integer parsedLenF = parsedLen;

        client.execute(() -> {
            if (normalizedF != null && !normalizedF.isEmpty()) {
                int L = normalizedF.length();
                PatternState.get().setLength(L);
                for (int i = 0; i < L; i++) {
                    char c = normalizedF.charAt(i);
                    PatternState.get().setChar(i, c == '_' ? '\0' : Character.toUpperCase(c));
                }
            } else if (parsedLenF != null) {
                PatternState.get().setLength(parsedLenF);
            }
            GTBHelper.updateSuggestionsAsync();
        });
    }
}
