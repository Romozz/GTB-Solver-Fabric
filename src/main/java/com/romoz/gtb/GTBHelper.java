package com.romoz.gtb;

import com.romoz.gtb.ui.SuggestionListWidget;
import com.romoz.gtb.ui.PatternState;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public final class GTBHelper {

    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final AtomicLong REQUEST_ID = new AtomicLong(0);
    private static volatile long lastAppliedRequest = -1;

    private GTBHelper() {}

    public static void updateSuggestionsAsync() {
        final long reqId = REQUEST_ID.incrementAndGet();
        final int len = PatternState.get().getLength();
        final char[] mask = PatternState.get().snapshot();
        final Pattern compiled = buildRegex(mask, len);

        new Thread(() -> {
            List<String> words = filterWords(compiled, len);
            if (reqId < REQUEST_ID.get()) return;
            MC.execute(() -> {
                if (reqId <= lastAppliedRequest) return;
                lastAppliedRequest = reqId;
                if (SuggestionListWidget.INSTANCE != null) {
                    SuggestionListWidget.INSTANCE.setItems(words);
                }
            });
        }, "GTB-FilterWorker").start();
    }

    public static Pattern buildRegex(char[] mask, int len) {
        if (len <= 0) len = mask != null ? mask.length : 0;
        StringBuilder sb = new StringBuilder(len + 4).append('^');
        for (int i = 0; i < len; i++) {
            char c = (mask != null && i < mask.length) ? mask[i] : '\0';
            if (c == '\0' || c == '_' || c == ' ') sb.append('.');
            else sb.append(Character.toUpperCase(c));
        }
        sb.append('$');
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static List<String> filterWords(Pattern regex, int len) {
        if (len <= 0) return Collections.emptyList();
        List<String> source = loadWords(len);
        if (source.isEmpty()) return Collections.emptyList();

        List<String> out = new ArrayList<>(Math.min(source.size(), 512));
        for (String w : source) {
            if (w == null) continue;
            String ww = w.trim();
            if (ww.length() != len) continue;
            if (regex.matcher(ww.toUpperCase(Locale.ROOT)).matches()) out.add(ww);
        }
        return out;
    }

    /**
     * Пытаемся вытащить слова из com.romoz.gtb.GTBWordList разными способами,
     * чтобы не зависеть от конкретного API.
     */
    @SuppressWarnings("unchecked")
    private static List<String> loadWords(int len) {
        try {
            Class<?> cls = Class.forName("com.romoz.gtb.GTBWordList");

            // 1) метод getBucket(int)
            try {
                Method m = cls.getDeclaredMethod("getBucket", int.class);
                Object r = m.invoke(null, len);
                if (r instanceof List) return (List<String>) r;
            } catch (NoSuchMethodException ignored) {}

            // 2) метод getWordsOfLength(int) / getWords(int)
            for (String name : new String[]{"getWordsOfLength", "getWords"}) {
                try {
                    Method m2 = cls.getDeclaredMethod(name, int.class);
                    Object r2 = m2.invoke(null, len);
                    if (r2 instanceof List) return (List<String>) r2;
                } catch (NoSuchMethodException ignored) {}
            }

            // 3) статическое поле WORDS (общий список) — отфильтруем по длине
            try {
                Field f = cls.getDeclaredField("WORDS");
                Object val = f.get(null);
                if (val instanceof List) return (List<String>) val;
            } catch (NoSuchFieldException ignored) {}

        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    /* Совместимость со старым вызовом из GTBSolver1 */
    public static List<String> getPossibleWords(String maskLike) {
        if (maskLike == null || maskLike.isEmpty()) return Collections.emptyList();
        String normalized = maskLike.replaceAll("\\s+", "");
        int len = normalized.length();
        char[] mask = normalized.toUpperCase(Locale.ROOT).toCharArray();
        Pattern p = buildRegex(mask, len);
        return filterWords(p, len);
    }
}
