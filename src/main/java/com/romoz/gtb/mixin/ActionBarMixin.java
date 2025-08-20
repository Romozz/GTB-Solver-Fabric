package com.romoz.gtb.mixin;

import com.romoz.gtb.ui.PatternState;
import com.romoz.gtb.GTBHelper;
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

    @Inject(method = "setOverlayMessage", at = @At("TAIL"))
    private void gtb$onOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        if (message == null) return;
        String raw = message.getString();
        if (raw == null || raw.isEmpty()) return;

        // 1) Попробуем найти «N Letters» / «Length: N»
        Matcher lenM = Pattern.compile("(?:\\b(?:len(?:gth)?|letters?)\\s*[:=-]?\\s*)(\\d{1,2})", Pattern.CASE_INSENSITIVE).matcher(raw);
        Integer parsedLen = null;
        if (lenM.find()) {
            parsedLen = Integer.parseInt(lenM.group(1));
        }

        // 2) Маска вида "__A_B" или "A _ _ _ Z"
        // Берём самую длинную последовательность символов [_A-Za-z ] с минимум 2 символами
        Matcher maskM = Pattern.compile("([_A-Za-z\\s]{2,})").matcher(raw);
        String best = null;
        while (maskM.find()) {
            String s = maskM.group(1).trim();
            // отфильтруем мусор, оставим те, где есть '_' или буквы, и нет цифр
            if (s.chars().anyMatch(ch -> ch == '_' || Character.isLetter(ch))) {
                if (best == null || s.length() > best.length()) best = s;
            }
        }

        if (best == null && parsedLen == null) return;

        // Нормализуем маску: удалим пробелы между слотами, если они есть
        String normalized = null;
        if (best != null) {
            normalized = best.replaceAll("\\s+", "");
            // оставим только буквы и '_'
            normalized = normalized.replaceAll("[^A-Za-z_]", "");
        }

        client.execute(() -> {
            PatternState st = PatternState.get();

            if (normalized != null && !normalized.isEmpty()) {
                int L = normalized.length();
                st.setLength(L);
                for (int i = 0; i < L; i++) {
                    char c = normalized.charAt(i);
                    st.setChar(i, c == '_' ? '\0' : Character.toUpperCase(c));
                }
            } else if (parsedLen != null) {
                st.setLength(parsedLen);
                // буквы оставим как были (или пустыми, если новая длина)
            }

            // обновить GUI-подсказки, если экран открыт
            GTBHelper.updateSuggestionsAsync();
        });
    }
}

