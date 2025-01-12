package com.romoz.gtb;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GTBHelper {

    // Регулярное выражение для разрешенных символов: кириллица, латиница, пробелы, _
    private static final Pattern VALID_CHARACTERS = Pattern.compile("[а-яА-Яa-zA-Z _]+");

    public static List<String> getPossibleWords(String pattern) {
        // Проверка, что строка содержит только допустимые символы
        if (!VALID_CHARACTERS.matcher(pattern).matches()) {
            return List.of(); // Вернуть пустой список, если есть недопустимые символы
        }

        // Убираем лишние пробелы и приводим шаблон к нижнему регистру
        String normalizedPattern = pattern.toLowerCase().trim();

        // Фильтруем слова из списка, которые подходят по длине и символам
        return GTBWordList.words.stream()
                .filter(word -> isMatch(normalizedPattern, word.toLowerCase()))
                .collect(Collectors.toList());
    }

    private static boolean isMatch(String pattern, String word) {
        // Проверяем, что длина совпадает
        if (pattern.length() != word.length()) {
            return false;
        }

        // Проверяем каждый символ
        for (int i = 0; i < pattern.length(); i++) {
            char patternChar = pattern.charAt(i);
            char wordChar = word.charAt(i);

            if (patternChar == '_') {
                continue; // Пропускаем символы `_`, они совпадают с любым символом
            }

            if (patternChar != wordChar) {
                return false; // Если символы не совпадают, слово не подходит
            }
        }

        return true;
    }
}
