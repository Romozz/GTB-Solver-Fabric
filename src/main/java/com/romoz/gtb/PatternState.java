package com.romoz.gtb.ui;

import java.util.Arrays;
import java.util.function.Consumer;

public class PatternState {
    private static final PatternState INSTANCE = new PatternState();

    private int length = 0;
    private char[] slots = new char[0];

    // Наблюдатель для GUI (обновлять экран при изменениях)
    private Consumer<PatternState> listener = s -> {};

    public static PatternState get() { return INSTANCE; }

    public void setListener(Consumer<PatternState> l) { this.listener = l != null ? l : s -> {}; }

    public int getLength() { return length; }
    public char[] getSlots() { return slots; }

    public void setLength(int len) {
        if (len < 1) len = 1;
        if (len > 64) len = 64;
        length = len;
        slots = new char[len];
        Arrays.fill(slots, '\0'); // \0 = неизвестно
        listener.accept(this);
    }

    public void setChar(int idx, char c) {
        if (idx < 0 || idx >= length) return;
        slots[idx] = c;
        listener.accept(this);
    }

    public void clear() {
        Arrays.fill(slots, '\0');
        listener.accept(this);
    }

    public String toPatternRegex() {
        StringBuilder sb = new StringBuilder(length);
        for (char c : slots) {
            if (c == '\0') sb.append('.'); // неизвестно
            else {
                // экранирование спецсимволов на всякий
                if ("\\.^$|?*+()[]{}".indexOf(c) >= 0) {
                    sb.append('\\');
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
