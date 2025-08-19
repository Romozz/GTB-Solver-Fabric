package com.romoz.gtb.mixin;

import com.romoz.gtb.ui.PatternState;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class ActionBarMixin {

    @Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"))
    private void gtbsolver$onActionBar(Text message, boolean tinted, CallbackInfo ci) {
        if (message == null) return;
        String raw = message.getString();
        // Пробуем распарсить и сразу применить к состоянию UI
        tryParseAndApply(raw, PatternState.get());
    }

    /**
     * Парсим action bar Hypixel и прямо обновляем состояние.
     * Возвращает true, если удалось распознать подсказку.
     */
    private boolean tryParseAndApply(String raw, PatternState st) {
        if (raw == null || raw.isEmpty()) return false;

        // Вариант 1: строка вида "__a____a_" (разрешаем буквы/подчёркивания/дефисы/пробелы)
        if (raw.matches("[A-Za-zА-Яа-я _-]+")) {
            String compact = raw.replace(" ", "").replace("-", "");
            int len = compact.length();
            if (len >= 1 && len <= 64) {
                st.setLength(len);
                for (int i = 0; i < len; i++) {
                    char ch = compact.charAt(i);
                    if (ch != '_') {
                        st.setChar(i, Character.toLowerCase(ch));
                    }
                }
                return true;
            }
        }

        // Вариант 2: "10 letters" (английский текст подсказки длины)
        String low = raw.toLowerCase();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+letters").matcher(low);
        if (m.find()) {
            int len = Integer.parseInt(m.group(1));
            if (len >= 1 && len <= 64) {
                st.setLength(len);
                st.clear();
                return true;
            }
        }

        return false;
    }
}
