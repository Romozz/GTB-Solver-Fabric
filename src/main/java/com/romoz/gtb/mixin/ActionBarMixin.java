package com.romoz.gtb.mixin;

import com.romoz.gtb.ui.PatternState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class ActionBarMixin {

    // Перехватываем показ action bar и пытаемся извлечь паттерн
    @Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V",
            at = @At("HEAD"))
    private void gtbsolver$onActionBar(Text message, boolean tinted, CallbackInfo ci) {
        if (message == null) return;
        String raw = message.getString();
        // Примеры: "__a____a_", "Theme: a________a", "10 letters"
        ParsedPattern p = parse(raw);
        if (p != null && p.length > 0) {
            PatternState st = PatternState.get();
            st.setLength(p.length);
            for (int i = 0; i < p.length; i++) {
                char c = p.known[i];
                if (c != '\0') st.setChar(i, c);
            }
        }
    }

    private static class ParsedPattern {
        final int length;
        final char[] known;
        ParsedPattern(int l, char[] k) { length = l; known = k; }
    }

    private ParsedPattern parse(String raw) {
        if (raw == null || raw.isEmpty()) return null;

        // 1) если строка вида "__a____a_"
        if (raw.matches("[A-Za-zА-Яа-я _-]+")) {
            String s = raw.replace(" ", "").replace("-", "");
            int len = s.length();
            if (len >= 2 && len <= 64) {
                char[] known = new char[len];
                for (int i = 0; i < len; i++) {
                    char ch = s.charAt(i);
                    known[i] = (ch == '_' ? '\0' : Character.toLowerCase(ch));
                }
                return new ParsedPattern(len, known);
            }
        }

        // 2) "10 letters"
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s+letters").matcher(raw.toLowerCase());
        if (m.find()) {
            int len = Integer.parseInt(m.group(1));
            if (len >= 1 && len <= 64) {
                return new ParsedPattern(len, new char[len]);
            }
        }
        return null;
    }
}
