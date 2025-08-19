package com.romoz.gtb.logic;

import com.romoz.gtb.GTBHelper;
import com.romoz.gtb.GTBWordList;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CandidatesProvider {

    // Собираем кандидатов через существующие утилиты проекта
    public static List<String> find(String regex, int length) {
        // 1) Получаем слова нужной длины
        List<String> pool = GTBWordList.getWordsOfLength(length);
        // 2) Фильтруем по regex
        List<String> filtered = pool.stream()
                .filter(w -> w.matches(regex))
                .collect(Collectors.toList());
        // 3) Ранжируем (пример: частота/алфавит)
        filtered.sort(Comparator
                .comparingInt((String w) -> -GTBHelper.scoreByFrequency(w))
                .thenComparing(w -> w));
        return filtered;
    }
}
