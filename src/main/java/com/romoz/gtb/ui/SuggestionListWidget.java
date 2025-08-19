package com.romoz.gtb.ui;

import com.romoz.gtb.GTBSolver; // твоя логика отправки слова в чат
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Прокручиваемый список подсказок в несколько колонок.
 * Совместим с 1.21.4 (DrawContext, без DrawableHelper/setScrollRate).
 * Поддерживает:
 * - setItems, getSelectedOrFirst, getCount, moveSelection (как ожидает GTBOverlayScreen)
 * - Клик по слову -> GTBSolver.handleWordClick(word, withArrows)
 * - Shift+клик -> "===== word ====="
 */
public class SuggestionListWidget extends ScrollableWidget {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<String> items = new ArrayList<>();
    private int selectedIndex = -1;

    // Метрики сетки
    private int rowHeight = 14;
    private int colGap    = 10;
    private int rowGap    = 2;
    private int colWidth  = 100;
    private int padding   = 6;

    // Цвета
    private static final int COLOR_BG       = 0x66000000;
    private static final int COLOR_HOVER    = 0x55FFFFFF;
    private static final int COLOR_TEXT     = 0xFFFFD700;
    private static final int COLOR_SELECTED = 0x5533AAFF;

    /** Конструктор, совместимый с твоим вызовом в GTBOverlayScreen:
     * new SuggestionListWidget(client, listW, listH, listTop, listTop + listH)
     * Здесь X примем = 10, Y = top, width = listW, height = (bottom - top).
     */
    public SuggestionListWidget(MinecraftClient client, int listW, int listH, int top, int bottom) {
        this(10, top, listW, Math.max(0, bottom - top));
    }

    /** Базовый конструктор с явными координатами. */
    public SuggestionListWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty());
        // В 1.21.4 setScrollRate(...) нет — скролл регулируем через getDeltaYPerScroll()
    }

    /* ===== API, которое ждёт GTBOverlayScreen ===== */

    public void setItems(List<String> list) {
        items.clear();
        if (list != null) items.addAll(list);
        selectedIndex = items.isEmpty() ? -1 : 0;
        setScrollY(0);
    }

    public String getSelectedOrFirst() {
        if (items.isEmpty()) return null;
        if (selectedIndex < 0 || selectedIndex >= items.size()) return items.get(0);
        return items.get(selectedIndex);
    }

    public int getCount() {
        return items.size();
    }

    public void moveSelection(int delta) {
        if (items.isEmpty()) { selectedIndex = -1; return; }
        selectedIndex = Math.max(0, Math.min(items.size() - 1, selectedIndex + delta));
        // авто-скролл чтобы выбранная строка была видна
        ensureSelectionVisible();
    }

    private void ensureSelectionVisible() {
        int innerX = getX() + padding;
        int innerY = getY() + padding;
        int innerW = getWidth() - padding * 2;
        int cols = Math.max(1, (innerW + colGap) / (colWidth + colGap));
        if (cols < 1) cols = 1;

        int row = (selectedIndex >= 0) ? (selectedIndex / cols) : 0;
        int itemY = innerY + row * (rowHeight + rowGap);
        int viewTop = getY();
        int viewBot = getBottom();

        int scrollY = (int)getScrollY();
        int itemTop = itemY - scrollY;
        int itemBot = itemTop + rowHeight;

        if (itemTop < viewTop + padding) {
            setScrollY(scrollY - ((viewTop + padding) - itemTop));
        } else if (itemBot > viewBot - padding) {
            setScrollY(scrollY + (itemBot - (viewBot - padding)));
        }
    }

    /* ===== Вёрстка/отрисовка ===== */

    /** Подстройка метрик по желанию */
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
        int rows = (int)Math.ceil(items.size() / (double)cols);
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

        for (int idx = 0; idx < items.size(); idx++) {
            int row = idx / cols;
            int col = idx % cols;

            int cellX = innerX + col * (colWidth + colGap);
            int cellY = startY + row * (rowHeight + rowGap);

            if (cellY + rowHeight < getY() || cellY > getBottom()) continue;

            String w = items.get(idx);

            // выделение выбранного
            if (idx == selectedIndex) {
                ctx.fill(cellX - 2, cellY - 1, cellX + colWidth + 2, cellY + rowHeight + 1, COLOR_SELECTED);
            }

            boolean hovered = mouseX >= cellX && mouseX <= cellX + colWidth &&
                              mouseY >= cellY && mouseY <= cellY + rowHeight;
            if (hovered) {
                ctx.fill(cellX - 2, cellY - 1, cellX + colWidth + 2, cellY + rowHeight + 1, COLOR_HOVER);
            }

            // усечение текста по ширине ячейки
            String shown = w;
            int maxPix = colWidth;
            while (tr.getWidth(shown) > maxPix && shown.length() > 1) {
                shown = shown.substring(0, shown.length() - 1);
            }
            if (!shown.equals(w) && shown.length() > 1) shown = shown.substring(0, shown.length() - 1) + "…";

            ctx.drawText(tr, shown, cellX, cellY, COLOR_TEXT, false);
        }
    }

    /* ===== Взаимодействие ===== */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        int innerX = getX() + padding;
        int innerY = getY() + padding;

        int innerW = getWidth() - padding * 2;
        int cols = Math.max(1, (innerW + colGap) / (colWidth + colGap));
        int startY = innerY - (int)getScrollY();

        for (int idx = 0; idx < items.size(); idx++) {
            int row = idx / cols;
            int col = idx % cols;

            int cellX = innerX + col * (colWidth + colGap);
            int cellY = startY + row * (rowHeight + rowGap);

            if (mouseX >= cellX && mouseX <= cellX + colWidth &&
                mouseY >= cellY && mouseY <= cellY + rowHeight) {

                selectedIndex = idx;
                boolean withArrows = Screen.hasShiftDown(); // 1.21.4 — статический метод
                // отправка в чат через твою реализацию
                GTBSolver.handleWordClick(items.get(idx), withArrows);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /* ===== Аудионаряды (доступность) — обязателен для ClickableWidget ===== */
    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        String s = getSelectedOrFirst();
        if (s != null) {
            builder.put(NarrationPart.TITLE, Text.literal("Подсказки. Выбрано: " + s));
        } else {
            builder.put(NarrationPart.TITLE, Text.literal("Подсказки пусты"));
        }
    }
}
