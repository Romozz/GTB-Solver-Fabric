package com.romoz.gtb.ui;

import com.romoz.gtb.GTBHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder; // <-- правильный импорт для 1.21.x
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;

public class CharSlotWidget extends ClickableWidget {
    private final int index;
    private final IntConsumer focusRequest;
    private char value = '\0';

    private int borderColorNormal = 0x80FFFFFF;
    private int borderColorFocused = 0xFFFFFFFF;
    private int fillColor = 0x55000000;
    private int textColor = 0xFFFFFFFF;
    private int underscoreColor = 0x80FFFFFF;

    public CharSlotWidget(int x, int y, int width, int height, int index, IntConsumer focusRequest) {
        super(x, y, width, height, Text.empty());
        this.index = index;
        this.focusRequest = focusRequest;
        char c = PatternState.get().getChar(index);
        if (c != '\0') this.value = Character.toUpperCase(c);
    }

    public boolean isEmpty() { return value == '\0'; }

    public void setValue(char c) {
        char newVal = Character.toUpperCase(c);
        if (!Character.isLetter(newVal)) return;
        this.value = newVal;
        PatternState.get().setChar(index, newVal);
        playDownSound(MinecraftClient.getInstance().getSoundManager());
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
        if (Character.isLetter(chr)) { setValue(chr); return true; }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!isEmpty()) {
                clearValue();
            } else if (index > 0) {
                focusRequest.accept(index - 1);
                char prev = PatternState.get().getChar(index - 1);
                if (prev != '\0') {
                    PatternState.get().setChar(index - 1, '\0');
                    GTBHelper.updateSuggestionsAsync();
                }
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (!isEmpty()) { clearValue(); return true; }
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT)  { focusRequest.accept(Math.max(0, index - 1)); return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) { focusRequest.accept(index + 1); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderButton(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x0 = getX(), y0 = getY(), x1 = x0 + getWidth(), y1 = y0 + getHeight();

        ctx.fill(x0, y0, x1, y1, fillColor);
        int border = isFocused() ? borderColorFocused : borderColorNormal;
        ctx.fill(x0, y0, x1, y0 + 1, border);
        ctx.fill(x0, y1 - 1, x1, y1, border);
        ctx.fill(x0, y0, x0 + 1, y1, border);
        ctx.fill(x1 - 1, y0, x1, y1, border);

        var tr = MinecraftClient.getInstance().textRenderer;
        String s = isEmpty() ? "_" : String.valueOf(value);
        int color = isEmpty() ? underscoreColor : textColor;
        int textW = tr.getWidth(s), textH = tr.fontHeight;
        int cx = x0 + (getWidth() - textW) / 2;
        int cy = y0 + (getHeight() - textH) / 2;
        ctx.drawText(tr, s, cx, cy, color, false);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // Ничего не озвучиваем
    }

    public void refreshFromState() {
        char c = PatternState.get().getChar(index);
        this.value = (c == '\0') ? '\0' : Character.toUpperCase(c);
    }
}
