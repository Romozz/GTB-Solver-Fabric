// !!! ОСТАВЬ СВОЙ package КАК В ТВОЁМ ПРОЕКТЕ !!!
package com.romoz.gtb.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * SuggestionListWidget — прокручиваемый список слов в несколько колонок.
 * - Видно сразу много слов (авто-колонки по ширине).
 * - Колёсико мыши — скролл.
 * - Клик по слову — отправка в чат (использует GTBSolver.handleWordClick).
 * - Shift+клик — отправка "===== word =====".
 */
public class SuggestionListWidget extends ScrollableWidget {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<String> words = new ArrayList<>();

    // Метрики сетки/вёрстки
    private int rowHeight = 14;  // высота строчки
    private int colGap    = 10;  // горизонтальный зазор между колонками
    private int rowGap    = 2;   // вертикальный зазор между строками
    private int colWidth  = 100; // минимальная ширина ячейки (подбирается под шрифт)
    private int padding   = 6;   // внутренние отступы

    // Цвета
    private static final int COLOR_BG       = 0x66000000; // полупрозрачный фон
    private static final int COLOR_HOVER    = 0x55FFFFFF; // подсветка hover
    private static final int COLOR_TEXT     = 0xFFFFD700; // «золотой» текст
    private static final int COLOR_COUNT    = 0xFFFFFFFF; // текст «Найдено: N»

    public SuggestionListWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty());
        setScrollRate(24.0);
    }

    /** Обновить список слов */
    public void setWords(List<String> list) {
        words.clear();
        if (list != null) words.addAll(list);
        setScrollY(0);
    }

    /** Тонкая настройка метрик (по желанию) */
    public void setMetrics(int colWidth, int rowHeight, int colGap, int rowGap, int padding) {
        this.colWidth = Math.max(60, colWidth);
        this.rowHeight = Math.max(12, rowHeight);
        this.colGap = Math.max(0, colGap);
        this.rowGap = Math.max(0, rowGap);
        this.padding = Math.max(0, padding);
    }

    @Override
    protected int getContentsHeight() {
        int innerW = getWidth() - padding * 2;
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
        // фон
        ctx.fill(getX(), getY(), getRight(), getBottom(), COLOR_BG);

        int innerX = getX() + padding;
        int innerY = getY() + padding;

        int innerW = getWidth() - padding * 2;
        int cols = Math.max(1, (innerW + colGap) / (colWidth + colGap));
        int startY = innerY - (int)getScrollY();

        var tr = mc.textRenderer;

        // заголовок «Найдено: N» (опционально)
        String countText = "Найдено: " + words.size();
        ctx.drawText(tr, countText, innerX, innerY - 12, COLOR_COUNT, false);

        for (int idx = 0; idx < words.size(); idx++) {
            int row = idx / cols;
            int col = idx % cols;

            int cellX = innerX + col * (colWidth + colGap);
            int cellY = startY + row * (rowHeight + rowGap);

            // пропустить, если ячейка вне видимой области
            if (cellY + rowHeight < getY() || cellY > getBottom()) continue;

            String w = words.get(idx);

            // hover
            boolean hovered = mouseX >= cellX && mouseX <= cellX + colWidth &&
                    mouseY >= cellY && mouseY <= cellY + rowHeight;
            if (hovered) {
                ctx.fill(cellX - 2, cellY - 1, cellX + colWidth + 2, cellY + rowHeight + 1, COLOR_HOVER);
            }

            // отрисовка текста с усечением "...", если не влезает
            String shown = w;
            int maxPix = colWidth;
            while (tr.getWidth(shown) > maxPix && shown.length() > 1) {
                shown = shown.substring(0, shown.length() - 1);
            }
            if (!shown.equals(w) && shown.length() > 1) shown = shown.substring(0, shown.length() - 1) + "…";

            ctx.drawText(tr, shown, cellX, cellY, COLOR_TEXT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        int innerX = getX() + padding;
        int innerY = getY() + padding;

        int innerW = getWidth() - padding * 2;
        int cols = Math.max(1, (innerW + colGap) / (colWidth + colGap));
        int startY = innerY - (int)getScrollY();

        for (int idx = 0; idx < words.size(); idx++) {
            int row = idx / cols;
            int col = idx % cols;

            int cellX = innerX + col * (colWidth + colGap);
            int cellY = startY + row * (rowHeight + rowGap);

            if (mouseX >= cellX && mouseX <= cellX + colWidth &&
                mouseY >= cellY && mouseY <= cellY + rowHeight) {

                // Shift+клик => "===== word ====="
                boolean withArrows = hasShiftDown();
                // вызываем уже существующую у тебя логику отправки в чат
                GTBSolver.handleWordClick(words.get(idx), withArrows);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
