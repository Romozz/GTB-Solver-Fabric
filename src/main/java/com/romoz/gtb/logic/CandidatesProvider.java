package com.romoz.gtb.logic;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CandidatesProvider {

    private static volatile List<String> DICT;

    public static List<String> find(String regex, int length) {
        List<String> all = getAllWords();
        if (all.isEmpty()) return Collections.emptyList();

        // компилируем регэксп один раз, игнорируя регистр
        Pattern p = Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        List<String> filtered = all.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(w -> w.length() == length)
                .filter(w -> p.matcher(w).matches())
                .distinct()
                .collect(Collectors.toList());

        // сортировка: алфавит как дефолт (если есть скоринг — подмешайте тут)
        filtered.sort(String::compareToIgnoreCase);
        return filtered;
    }

    // ===== словарь через рефлексию + безопасные фоллбеки =====
    @SuppressWarnings("unchecked")
    private static List<String> getAllWords() {
        if (DICT != null) return DICT;

        List<String> result = new ArrayList<>();
        try {
            Class<?> cls = Class.forName("com.romoz.gtb.GTBWordList");

            // 1) наиболее вероятные статические поля-коллекции
            for (String fieldName : new String[]{"WORDS", "WORD_LIST", "ALL", "DICTIONARY", "LIST"}) {
                try {
                    Field f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object v = f.get(null);
                    addCollection(result, v);
                } catch (NoSuchFieldException ignored) {}
            }

            // 2) методы без аргументов, возвращающие коллекцию/массив
            if (result.isEmpty()) {
                for (String mName : new String[]{"getAll", "getWords", "words", "all", "asList"}) {
                    for (Method m : cls.getDeclaredMethods()) {
                        if (!m.getName().equals(mName)) continue;
                        if (m.getParameterCount() == 0) {
                            m.setAccessible(true);
                            Object v = m.invoke(null);
                            addCollection(result, v);
                        }
                    }
                }
            }

            // 3) метод по длине: getWordsOfLength(int) — если есть, соберём слитьё по длинам
            if (result.isEmpty()) {
                try {
                    Method m = cls.getDeclaredMethod("getWordsOfLength", int.class);
                    m.setAccessible(true);
                    // собираем длины от 1 до 32 (разумная граница)
                    for (int L = 1; L <= 32; L++) {
                        Object v = m.invoke(null, L);
                        addCollection(result, v);
                    }
                } catch (NoSuchMethodException ignored) {}
            }

        } catch (Throwable ignored) {}

        // финализация
        DICT = Collections.unmodifiableList(result.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList()));
        return DICT;
    }

    private static void addCollection(List<String> out, Object v) {
        if (v == null) return;
        if (v instanceof Collection) {
            for (Object o : (Collection<?>) v) if (o != null) out.add(o.toString());
        } else if (v.getClass().isArray()) {
            int n = java.lang.reflect.Array.getLength(v);
            for (int i = 0; i < n; i++) {
                Object o = java.lang.reflect.Array.get(v, i);
                if (o != null) out.add(o.toString());
            }
        }
    }
}
