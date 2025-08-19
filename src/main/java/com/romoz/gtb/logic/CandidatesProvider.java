package com.romoz.gtb.logic;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class CandidatesProvider {

    // Кэшируем пул слов (лениво)
    private static volatile List<String> DICT;

    public static List<String> find(String regex, int length) {
        List<String> all = getAllWords();
        if (all.isEmpty()) return Collections.emptyList();

        // 1) фильтрация по длине и regex
        List<String> filtered = all.stream()
                .filter(w -> w != null)
                .map(String::trim)
                .filter(w -> w.length() == length)
                .filter(w -> w.matches(regex))
                .collect(Collectors.toList());

        // 2) ранжирование: сперва по скорингу (если доступен), затем по алфавиту
        Comparator<String> cmp = Comparator
                .comparingInt((String w) -> -scoreByFrequencySafe(w))
                .thenComparing(Comparator.naturalOrder());

        filtered.sort(cmp);
        return filtered;
    }

    // ===== Внутреннее: словарь через рефлексию =====
    @SuppressWarnings("unchecked")
    private static List<String> getAllWords() {
        if (DICT != null) return DICT;

        List<String> result = new ArrayList<>();
        try {
            Class<?> cls = Class.forName("com.romoz.gtb.GTBWordList");

            // Попробуем поля-коллекции
            for (String fieldName : new String[]{"WORDS", "WORD_LIST", "ALL", "DICTIONARY"}) {
                try {
                    Field f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object v = f.get(null);
                    if (v instanceof Collection) {
                        ((Collection<?>) v).forEach(o -> { if (o != null) result.add(o.toString()); });
                    }
                } catch (NoSuchFieldException ignored) {}
            }

            // Попробуем статические методы, возвращающие коллекцию
            if (result.isEmpty()) {
                for (String mName : new String[]{"getAll", "getWords", "words", "all"}) {
                    try {
                        Method m = cls.getDeclaredMethod(mName);
                        m.setAccessible(true);
                        Object v = m.invoke(null);
                        if (v instanceof Collection) {
                            ((Collection<?>) v).forEach(o -> { if (o != null) result.add(o.toString()); });
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
            }

        } catch (Throwable t) {
            // проглатываем — вернём пусто
        }

        DICT = Collections.unmodifiableList(result.stream()
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList()));
        return DICT;
    }

    // ===== Внутреннее: скоринг через рефлексию (опционально) =====
    private static int scoreByFrequencySafe(String word) {
        try {
            Class<?> h = Class.forName("com.romoz.gtb.GTBHelper");
            // Ищем метод со строкой на входе и int на выходе
            for (String mName : new String[]{"scoreByFrequency", "score", "freq", "frequencyScore"}) {
                for (Method m : h.getDeclaredMethods()) {
                    if (!m.getName().equals(mName)) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 1 && p[0] == String.class && m.getReturnType() == int.class) {
                        m.setAccessible(true);
                        return (int) m.invoke(null, word);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 0; // если нет метода — нейтральный скор
    }
}
