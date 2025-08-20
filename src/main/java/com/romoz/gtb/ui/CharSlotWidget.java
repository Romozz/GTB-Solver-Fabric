package com.romoz.gtb.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;

/**
 * Однобуквенный слот ввода для паттерна слова.
 * Фичи:
 *  - ввод буквы сразу прыгает на следующий слот
 *  - Backspace чистит текущий, а если он пуст — едет влево и чистит там
 *  - Delete чистит текущий
 *  - Стрелки ←/→ двигают фокус
 *  - визуально пустое = "_"
 *
 * Требования к проекту:
 *  - PatternState.get().setChar(index, c) / getChar(index) / getLength()
 *  - GTBHelper.updateSuggestionsAsync()
 */
public class CharSlotWidget extends ClickableWidget {

    private final int index;
    private final IntConsumer focusRequest;
    private char value = '\0';

    // визуальные настройки
    private int paddingX = 4;
    private int paddingY = 3;
    private int borderColorNormal = 0x80FFFFFF;
    private int borderColorFocused = 0xFFFFFFFF;
    private int fillColor = 0x55000000;
    private int textColor = 0xFFFFFFFF;
    private int underscoreColor = 0x80FFFFFF;

    public CharSlotWidget(int x, int y, int width, int height, int index, IntConsumer focusRequest) {
        super(x, y, width, height, Text.empty());
        this.index = index;
        this.focusRequest = focusRequest;
        // Подтянуть сохранённое значение из state при создании
        char c = PatternState.get().getChar(index);
        if (c != '\0') this.value = Character.toUpperCase(c);
    }

    public int getIndex() {
        return index;
    }

    public char getValue() {
        return value;
    }

    public boolean isEmpty() {
        return value == '\0';
    }

    public void setValue(char c) {
        char newVal = Character.toUpperCase(c);
        if (!Character.isLetter(newVal)) return;
        this.value = newVal;
        PatternState.get().setChar(index, newVal);
        playDownSound(MinecraftClient.getInstance().getSoundManager());
        // авто-прыжок вперёд
        focusRequest.accept(index + 1);
        GTBHelper.updateSuggestionsAsync();
    }

    public void clearValue() {
        if (this.value != '\0') {
            this.value = '\0';
            PatternState.get().setChar(index, '\0');
            GTBHelper.updateSuggestionsAsync();
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (Character.isLetter(chr)) {
            setValue(chr);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Backspace
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!isEmpty()) {
                clearValue();
            } else {
                // если пусто — едем влево и там очищаем, если было значение
                if (index > 0) {
                    focusRequest.accept(index - 1);
                    char prev = PatternState.get().getChar(index - 1);
                    if (prev != '\0') {
                        PatternState.get().setChar(index - 1, '\0');
                        GTBHelper.updateSuggestionsAsync();
                    }
                }
            }
            return true;
        }
        // Delete
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (!isEmpty()) {
                clearValue();
                return true;
            }
        }
        // Стрелки
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            focusRequest.accept(Math.max(0, index - 1));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            focusRequest.accept(index + 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = this.clicked(mouseX, mouseY);
        if (inside) {
            setFocused(true);
            focusRequest.accept(index);
            playDownSound(MinecraftClient.getInstance().getSoundManager());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderButton(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + getWidth();
        int y1 = y0 + getHeight();

        // фон
        ctx.fill(x0, y0, x1, y1, fillColor);

        // рамка (толщина 1px)
        int border = isFocused() ? borderColorFocused : borderColorNormal;
        // верх/низ
        ctx.fill(x0, y0, x1, y0 + 1, border);
        ctx.fill(x0, y1 - 1, x1, y1, border);
        // лево/право
        ctx.fill(x0, y0, x0 + 1, y1, border);
        ctx.fill(x1 - 1, y0, x1, y1, border);

        // текст
        var tr = MinecraftClient.getInstance().textRenderer;
        String s = isEmpty() ? "_" : String.valueOf(value);
        int color = isEmpty() ? underscoreColor : textColor;

        int textW = tr.getWidth(s);
        int textH = tr.fontHeight;

        int cx = x0 + (getWidth() - textW) / 2;
        int cy = y0 + (getHeight() - textH) / 2;

        ctx.drawText(tr, s, cx, cy, color, false);
    }

    // Позволяет внешнему коду принудительно выставить букву (например, после парсинга actionbar)
    public void applyExternal(char c) {
        char normalized = (c == '\0') ? '\0' : Character.toUpperCase(c);
        if (normalized == '\0') {
            this.value = '\0';
        } else if (Character.isLetter(normalized)) {
            this.value = normalized;
        }
    }

    // Хелпер: вызвать при перестроении экрана после изменения длины
    public void refreshFromState() {
        char c = PatternState.get().getChar(index);
        this.value = (c == '\0') ? '\0' : Character.toUpperCase(c);
    }

    // Опционально: настроить визуальные параметры из кода
    public CharSlotWidget padding(int px, int py) { this.paddingX = px; this.paddingY = py; return this; }
    public CharSlotWidget colors(int borderNormal, int borderFocused, int fill, int text, int underscore) {
        this.borderColorNormal = borderNormal;
        this.borderColorFocused = borderFocused;
        this.fillColor = fill;
        this.textColor = text;
        this.underscoreColor = underscore;
        return this;
    }
}
