package com.romoz.gtb.logic;

import com.romoz.gtb.GTBWordList;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class CandidatesProvider {

    // Берём напрямую из GTBWordList и сразу нормализуем (trim + dedup)
    private static final List<String> WORDS = GTBWordList.words.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(Collectors.toUnmodifiableList());

    private CandidatesProvider() {}

    /**
     * Фильтрует слова по regex (полное совпадение) и длине.
     * @param regex  шаблон без якорей (якоря добавляются внутри)
     * @param length требуемая длина строки (включая пробелы/дефисы)
     */
    public static List<String> find(String regex, int length) {
        if (WORDS.isEmpty()) return Collections.emptyList();

        // компилируем регэксп один раз, игнорируя регистр
        Pattern p = Pattern.compile("^" + regex + "$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        return WORDS.stream()
                .filter(w -> w.length() == length)
                .filter(w -> p.matcher(w).matches())
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }
}
