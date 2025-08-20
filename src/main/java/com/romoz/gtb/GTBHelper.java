package com.romoz.gtb;

import com.romoz.gtb.ui.SuggestionListWidget;
import com.romoz.gtb.words.GTBWordList;
import com.romoz.gtb.state.PatternState;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Вспомогательная логика GTB:
 *  - построение регэкспа по текущей маске из PatternState
 *  - фильтрация слов
 *  - безопасное обновление списка подсказок на клиентском треде
 *
 * Важно:
 *  - НЕ изменяет PatternState (только читает snapshot).
 *  - Дебаунсит частые вызовы updateSuggestionsAsync().
 */
public final class GTBHelper {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    // простой дебаунс по "поколению" запросов: берём в работу только последний
    private static final AtomicLong REQUEST_ID = new AtomicLong(0);
    private static volatile long lastAppliedRequest = -1;

    private GTBHelper() {}

    /**
     * Запускает пересчёт подсказок под текущий PatternState.
     * Вызывать после любого изменения слотов/длины или парсинга actionbar.
     */
    public static void updateSuggestionsAsync() {
        final long reqId = REQUEST_ID.incrementAndGet();

        // Снимем моментальное состояние — БЕЗ мутации
        final int len = PatternState.get().getLength();
        final char[] mask = PatternState.get().snapshot();

        // Строим регэксп прямо тут (дёшево)
        final Pattern compiled = buildRegex(mask, len);

        // Фильтрация может быть «тяжёлой» для больших словарей — вынесем из главного треда
        new Thread(() -> {
            List<String> words = filterWords(compiled, len);
            // Публикуем только если это всё ещё актуальный запрос
            if (reqId < REQUEST_ID.get()) return;

            // На клиентском треде обновим список
            MC.execute(() -> {
                if (reqId <= lastAppliedRequest) return; // лишняя защита
                lastAppliedRequest = reqId;

                SuggestionListWidget list = SuggestionListWidget.get();
                if (list != null) {
                    list.setItems(words);
                }
            });
        }, "GTB-FilterWorker").start();
    }

    /**
     * Построить regex вида ^A.B..$ где '.' = пустой слот.
     * Дополнительно нормализует буквы к верхнему регистру.
     */
    public static Pattern buildRegex(char[] mask, int len) {
        if (len <= 0) len = mask != null ? mask.length : 0;
        StringBuilder sb = new StringBuilder(len + 4);
        sb.append('^');
        for (int i = 0; i < len; i++) {
            char c = (mask != null && i < mask.length) ? mask[i] : '\0';
            if (c == '\0' || c == '_' || c == ' ') {
                sb.append('.');
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }
        sb.append('$');
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /**
     * Применяет фильтр к словарю.
     * NB: НЕ меняет порядок, но можно добавить сортировку по длине/алфавиту при желании.
     */
    private static List<String> filterWords(Pattern regex, int len) {
        if (len <= 0) return Collections.emptyList();

        List<String> bucket = GTBWordList.getBucket(len);
        if (bucket == null || bucket.isEmpty()) return Collections.emptyList();

        List<String> out = new ArrayList<>(Math.min(bucket.size(), 512));
        for (String w : bucket) {
            if (w == null) continue;
            String ww = w.trim();
            if (ww.length() != len) continue;
            if (regex.matcher(ww.toUpperCase(Locale.ROOT)).matches()) {
                out.add(ww);
            }
        }
        return out;
    }

    /* ===================== УТИЛИТЫ (опционально) ===================== */

    /**
     * Прямая фильтрация по произвольной маске (например, пришедшей из actionbar),
     * не трогая PatternState.
     */
    public static List<String> previewByMask(String maskLike) {
        if (maskLike == null || maskLike.isEmpty()) return Collections.emptyList();
        String normalized = maskLike.replaceAll("\\s+", "");
        int len = normalized.length();
        char[] mask = normalized.toUpperCase(Locale.ROOT).toCharArray();
        Pattern p = buildRegex(mask, len);
        return filterWords(p, len);
    }
}
