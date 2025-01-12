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

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class GTBSolver implements ModInitializer {

    public static final String PREFIX = "§6§lGTBSolver §8» ";

    @Override
    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("gtb")
                            .then(argument("pattern", StringArgumentType.greedyString())
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
                                    }))
            );
        });
    }

    // Отправка ошибки с дополнительным текстом
    public static void sendErrorFeedback(String message, String details) {
        MutableText errorText = Text.literal(PREFIX + message)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555))); // Красный цвет для основной ошибки
        MutableText detailedText = Text.literal(details)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAAAA))); // Более светлый красный для детали

        MinecraftClient.getInstance().player.sendMessage(errorText.append(detailedText), false);
    }

    // Отправка успешного сообщения с кликабельными словами
    public static void sendClickableWords(String pattern, List<String> words) {
        MutableText message = Text.literal(PREFIX)
                .append(Text.literal("Possible words for '").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xD3D3D3)))) // Светлый серый для текста "Possible words for"
                .append(Text.literal(pattern).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xD3D3D3)))) // Светлый серый для паттерна
                .append(Text.literal("':").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xD3D3D3)))); // Светлый серый для ":"

        for (String word : words) {
            // Используем более светлый желтый (цвет #FFFF99) для кликабельных слов
            MutableText clickableWord = Text.literal(word)
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF99)) // Светло-желтый для кликабельных слов
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, word)) // При клике вставляется слово
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to insert: " + word)
                                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00)))) // Подсказка с цветом
                            )
                    );

            message.append(Text.literal(", ")).append(clickableWord); // Добавляем пробел между словами
        }

        // Добавляем автора в сообщение
        MutableText authorText = Text.literal(" (Author: Romoz)").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x6A5ACD))); // Фиолетовый цвет для автора
        message.append(authorText); // Добавляем текст об авторе в конце сообщения

        // Отправляем итоговое сообщение
        MinecraftClient.getInstance().player.sendMessage(message, false);
    }
}
