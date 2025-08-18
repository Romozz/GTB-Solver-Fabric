package com.romoz.gtb;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.Style;
import net.minecraft.text.HoverEvent;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class GTBSolver implements ModInitializer {
    public static final String PREFIX = "§6§lGTBSolver §8» ";
    private static final Map<String, Long> lastClickTimes = new HashMap<>(); // Хранение времени последнего клика по слову
    private static final long CLICK_DELAY = 4000; // Задержка в 3 секунды (в миллисекундах)

    @Override
    public void onInitialize() {
        // Регистрация команд
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("gtb")
                            .then(argument("pattern", StringArgumentType.greedyString()) // Используем greedyString(), чтобы принимать любые символы
                                    .executes(context -> {
                                        String pattern = StringArgumentType.getString(context, "pattern");
                                        List<String> words = GTBHelper.getPossibleWords(pattern);

                                        if (words.isEmpty()) {
                                            sendErrorFeedback("No matching words for: " + pattern, "Try adjusting the pattern or check the input.");
                                            return 0;
                                        }

                                        // Отправляем сообщение с кликабельными словами
                                        sendClickableWords(pattern, words);
                                        return 1;
                                    })
                            )
            );

            // Команда для клика по слову
            dispatcher.register(
                    literal("gtbsolver:click")
                            .then(argument("arrows", StringArgumentType.string())  // 1-й аргумент — стрелочки (true/false)
                                    .then(argument("word", StringArgumentType.greedyString()) // 2-й аргумент — слово или паттерн
                                            .executes(context -> {
                                                String arrowsArg = StringArgumentType.getString(context, "arrows");
                                                String words = StringArgumentType.getString(context, "word");

                                                // Проверяем, нужно ли добавлять стрелки (true/false)
                                                boolean withArrows = "true".equalsIgnoreCase(arrowsArg);

                                                handleWordClick(words, withArrows); // Вызовем обработку клика
                                                return 1;
                                            })
                                    )
                            )
            );
        });
    }

    // Обработка клика по слову с учетом задержки и отправка слова от имени игрока
    public static void handleWordClick(String word, boolean withArrows) {
        long currentTime = System.currentTimeMillis();
        long lastClickTime = lastClickTimes.getOrDefault(word, 0L);

        if (currentTime - lastClickTime < CLICK_DELAY) {
            // Если прошло менее 3 секунд с последнего клика
            sendFeedback("You must wait 4 seconds before clicking this word again.");
        } else {
            // Обновляем время последнего клика
            lastClickTimes.put(word, currentTime);

            // Отправляем сообщение с выбранным словом в чат и на сервер
            sendWordToChat(word, withArrows);
        }
    }

    // Отправка выбранного слова в чат (с учетом стрелочек)
    public static void sendWordToChat(String word, boolean withArrows) {
        if (MinecraftClient.getInstance().player != null) {
            String formattedWord = withArrows ? "===== " + word + " =====" : word;
            // Отправляем сообщение от имени игрока
            MinecraftClient.getInstance().player.networkHandler.sendChatMessage(formattedWord);
        }
    }

    // Отправка ошибок в чат
    public static void sendErrorFeedback(String message, String details) {
        MutableText errorText = Text.literal(PREFIX + message)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555))); // Красный для ошибки
        MutableText detailedText = Text.literal(details)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAAAA))); // Легкий красный для деталей

        MinecraftClient.getInstance().player.sendMessage(errorText.append(detailedText), false);
    }

    // Отправка кликабельных слов с плюсиками в скобках и задержкой
    public static void sendClickableWords(String pattern, List<String> words) {
        MutableText message = Text.literal(PREFIX)
                .append(Text.literal("Possible words for: " + pattern).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xD3D3D3)))); // Оформление

        for (String word : words) {
            String lowerWord = word.toLowerCase();
            // Выбираем насыщенный желтый цвет для слов
            MutableText clickableWord = Text.literal(lowerWord)
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00)) // Яркий желтый цвет для слова
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gtbsolver:click false " + lowerWord)) // Команда клика для обычного слова
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to insert: " + lowerWord)
                                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00))))));

            // Плюсик в скобках другого цвета
            MutableText plusSign = Text.literal(" (+)")
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00FF00)) // Зеленый цвет для плюсика
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gtbsolver:click true " + lowerWord))); // Команда клика с добавлением стрелочек

            message.append(Text.literal(", ")).append(clickableWord).append(plusSign); // Разделители между словами и плюсиками
        }

        // Добавляем автора
        MutableText authorText = Text.literal(" (Author: Romoz)").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x6A5ACD)));
        message.append(authorText);

        MinecraftClient.getInstance().player.sendMessage(message, false);
    }

    // Отправка сообщения в чат
    public static void sendFeedback(String message) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal(PREFIX + message), false);
        }
    }
}

