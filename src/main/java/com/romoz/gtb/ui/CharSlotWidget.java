package com.romoz.gtb.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class CharSlotWidget extends ClickableWidget {

    private char value = '\0';
    private final int index;
    private final Consumer<Character> onChange;

    public CharSlotWidget(net.minecraft.client.font.TextRenderer tr, int x, int y, int w, int h, int index, Consumer<Character> onChange) {
        super(x, y, w, h, Text.of(""));
        this.index = index;
        this.onChange = onChange;
    }

    @Override
    protected void renderWidget(DrawContext dc, int mouseX, int mouseY, float delta) {
        int bg = isFocused() ? 0xFF2E2E2E : 0xFF1E1E1E;
        dc.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        String s = value == '\0' ? "_" : String.valueOf(value);
        int tw = this.getMessage().getString().length(); // unused but avoids warnings
        dc.drawCenteredTextWithShadow(this.textRenderer, s, getX() + getWidth()/2, getY() + (getHeight()-8)/2, 0xFFFFFF);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Разрешаем буквы и дефис/пробел как фиксированные символы
        if (Character.isLetter(chr) || chr == '-' || chr == ' ') {
            value = normalize(chr);
            onChange.accept(value);
            return true;
        }
        if (chr == '\b') { value = '\0'; onChange.accept(value); return true; } // backspace
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean r = super.mouseClicked(mouseX, mouseY, button);
        this.setFocused(true);
        return r;
    }

    private char normalize(char c) {
        // Нормализация: нижний регистр, поддержка латиницы/кириллицы
        return Character.toLowerCase(c);
    }
}
