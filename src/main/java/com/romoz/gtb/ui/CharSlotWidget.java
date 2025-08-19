package com.romoz.gtb.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class CharSlotWidget extends ClickableWidget {

    private final TextRenderer tr;
    private char value = '\0';
    private final int index;
    private final Consumer<Character> onChange;

    public CharSlotWidget(TextRenderer tr, int x, int y, int w, int h, int index, Consumer<Character> onChange) {
        super(x, y, w, h, Text.empty());
        this.tr = tr;
        this.index = index;
        this.onChange = onChange;
    }

    @Override
    protected void renderWidget(DrawContext dc, int mouseX, int mouseY, float delta) {
        int bg = isFocused() ? 0xFF2E2E2E : 0xFF1E1E1E;
        dc.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        String s = value == '\0' ? "_" : String.valueOf(value);
        int cx = getX() + getWidth() / 2;
        int cy = getY() + (getHeight() - 8) / 2;
        dc.drawCenteredTextWithShadow(tr, s, cx, cy, 0xFFFFFF);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (Character.isLetter(chr) || chr == '-' || chr == ' ') {
            value = Character.toLowerCase(chr);
            onChange.accept(value);
            return true;
        }
        if (chr == '\b') {
            value = '\0';
            onChange.accept(value);
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationMessageBuilder.RequiredNarrationPart.TITLE,
                Text.literal("GTB slot " + (index + 1) + ": " + (value == '\0' ? "empty" : value)));
    }
}
