package com.romoz.gtb.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Простой прокручиваемый «листинг» слов в несколько колонок.
 * - Показывает много слов сразу.
 * - Клик по слову вызывает onWordClick.accept(word, withArrows).
 * - Shift+клик — отправка с «стрелками» (===== word =====).
 */
public class WordListWidget extends ScrollableWidget {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<String> words = new ArrayList<>();
    private final BiConsumer<String, Boolean> onWordClick;

    private int rowHeight = 14;       // высота строки
    private int colGap = 10;          // горизонтальный зазор между колонками
    private int rowGap = 2;           // вертикальный зазор между строками
    private int colWidth = 90;        // ширина «ячейки» (подбирай под свой шрифт)
    private int padding = 6;          // внутренние отступы

    public WordListWidget(int x, int y, int width, int height, BiConsumer<String, Boolean> onWordClick) {
        super(x, y, width, height, Text.empty());
        this.onWordClick = onWordClick;
        this.setScrollRate(24.0);
    }

    public void setWords(List<String> list) {
        this.words.clear();
        if (list != null) this.words.addAll(list);
        this.setScrollY(0);
    }

    public void setCellMetrics(int colWidth, int rowHeight) {
        this.colWidth = Math.max(60, colWidth);
        this.rowHeight = Math.max(12, rowHeight);
    }

    public void setGaps(int colGap, int rowGap, int padding) {
        this.colGap = Math.max(0, colGap);
        this.rowGap = Math.max(0, rowGap);
        this.padding = Math.max(0, padding);
    }

    @Override
    protected int getContentsHeight() {
        int innerW = this.getWidth() - padding * 2;
        int cols = Math.max(1, (innerW + colGap) / (colWidth + colGap));
        int rows = (int)Math.ceil(words.size() / (double)cols);
        return padding * 2 + rows * rowHeight + Math.max(0, rows - 1) * rowGap;
    }

    @Override
    protected double getDeltaYPerScroll() {
        return 12.0;
    }

    @Override
    protected void renderContents(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int innerX = this.getX() + padding;
        int innerY = this.getY() + padding;

        int innerW = this.getWidth() - padding * 2;
        int cols = Math.max(1, (innerW + colGap) / (colWidth + colGap));

        int startY = innerY - (int)this.getScrollY();

        // фон (не обязателен, просто чтобы список читался)
        ctx.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x66000000);

        for (int idx = 0; idx < words.size(); idx++) {
            int row = idx / cols;
            int col = idx % cols;

            int cellX = innerX + col * (colWidth + colGap);
            int cellY = startY + row * (rowHeight + rowGap);

            // пропускаем, если «ячейка» вне видимой области
            if (cellY + rowHeight < this.getY() || cellY > this.getBottom()) continue;

            String w = words.get(idx);

            // подсветка hover
            boolean hovered = mouseX >= cellX && mouseX <= cellX + colWidth &&
                              mouseY >= cellY && mouseY <= cellY + rowHeight;
            if (hovered) {
                ctx.fill(cellX - 2, cellY - 1, cellX + colWidth + 2, cellY + rowHeight + 1, 0x55FFFFFF);
            }

            // рисуем текст (обрежем, если не влезает)
            String shown = w;
            int maxPix = colWidth;
            var tr = mc.textRenderer;
            while (tr.getWidth(shown) > maxPix && shown.length() > 1) {
                shown = shown.substring(0, shown.length() - 1);
            }
            if (!shown.equals(w) && shown.length() > 1) shown = shown.substring(0, shown.length() - 1) + "…";

            ctx.drawText(tr, shown, cellX, cellY, 0xFFFFD700, false); // «золотой» цвет
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        int innerX = this.getX() + padding;
        int innerY = this.getY() + padding;

        int innerW = this.getWidth() - padding * 2;
        int cols = Math.max(1, (innerW + colGap) / (colWidth + colGap));

        int startY = innerY - (int)this.getScrollY();

        for (int idx = 0; idx < words.size(); idx++) {
            int row = idx / cols;
            int col = idx % cols;

            int cellX = innerX + col * (colWidth + colGap);
            int cellY = startY + row * (rowHeight + rowGap);

            if (mouseX >= cellX && mouseX <= cellX + colWidth &&
                mouseY >= cellY && mouseY <= cellY + rowHeight) {

                boolean withArrows = Screen.hasShiftDown(); // Shift+клик => «===== word =====»
                onWordClick.accept(words.get(idx), withArrows);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // Не обязательные, но корректные заглушки для навигации/доступности:
    @Override public GuiNavigationPath getNavigationPath(GuiNavigation nav) { return null; }
    @Override public void setFocused(boolean focused) { super.setFocused(focused); }
}
