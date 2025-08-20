package com.romoz.gtb.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Экран оверлея GTB Solver.
 * Особенности:
 *  - Восстанавливает состояние длины и букв из PatternState при открытии
 *  - Не сбрасывает визуальный ввод при обновлении подсказок
 *  - Автофокус на первый пустой слот
 *  - Кнопки для изменения длины (−/+), перестраивание слотов без потери набранного
 *
 * Требует:
 *  - CharSlotWidget (с авто-прыжком вперёд и Backspace логикой)
 *  - PatternState (getLength, setLength, getChar, setChar, snapshot)
 *  - GTBHelper.updateSuggestionsAsync()
 *  - SuggestionListWidget (отображение списка слов)
 */
public class GTBOverlayScreen extends Screen {

    private static final int MIN_LEN = 1;
    private static final int MAX_LEN = 25;

    private final List<CharSlotWidget> slots = new ArrayList<>();
    private SuggestionListWidget suggestionList;

    // Геометрия слотов и контролов
    private int slotWidth = 18;
    private int slotHeight = 22;
    private int slotGap = 4;
    private int topPad = 30;
    private int controlsPad = 8;

    private ButtonWidget minusBtn;
    private ButtonWidget plusBtn;
    private ButtonWidget clearBtn;
    private ButtonWidget closeBtn;

    public GTBOverlayScreen() {
        super(Text.literal("GTB Solver"));
    }

    @Override
    protected void init() {
        this.clearChildren(); // на случай повторной инициализации
        slots.clear();

        // Кнопки управления длиной и очисткой
        int btnY = topPad - 24;
        int btnW = 20;
        int btnH = 20;

        minusBtn = ButtonWidget.builder(Text.literal("−"), b -> onLengthChanged(PatternState.get().getLength() - 1))
                .dimensions(controlsPad, btnY, btnW, btnH).build();
        plusBtn = ButtonWidget.builder(Text.literal("+"), b -> onLengthChanged(PatternState.get().getLength() + 1))
                .dimensions(controlsPad + btnW + 4, btnY, btnW, btnH).build();

        clearBtn = ButtonWidget.builder(Text.literal("Очистить"), b -> {
                    PatternState.get().clearAll();
                    // просто обновим поле без потери длины
                    rebuildSlots(); // перерисуем значения
                    focusFirstEmpty();
                    GTBHelper.updateSuggestionsAsync();
                })
                .dimensions(controlsPad + (btnW + 4) * 2 + 6, btnY, 70, btnH).build();

        closeBtn = ButtonWidget.builder(Text.literal("Закрыть"), b -> onClose())
                .dimensions(width - controlsPad - 72, btnY, 72, btnH).build();

        addDrawableChild(minusBtn);
        addDrawableChild(plusBtn);
        addDrawableChild(clearBtn);
        addDrawableChild(closeBtn);

        // Слоты символов
        rebuildSlots();

        // Список подсказок (ниже слотов)
        int listTop = calcSlotsY() + slotHeight + 14;
        int listHeight = Math.max(40, height - listTop - 10);
        suggestionList = new SuggestionListWidget(client, width - controlsPad * 2, listHeight,
                listTop, controlsPad, width - controlsPad);
        addSelectableChild(suggestionList);

        // Запуск пересчёта под уже известный паттерн
        GTBHelper.updateSuggestionsAsync();
    }

    private void rebuildSlots() {
        // удалить прежние CharSlotWidget из children
        this.children().removeIf(c -> c instanceof CharSlotWidget);
        this.drawables.removeIf(d -> d instanceof CharSlotWidget);
        this.selectables.removeIf(s -> s instanceof CharSlotWidget);
        slots.clear();

        int len = PatternState.get().getLength();
        if (len < MIN_LEN) len = MIN_LEN;
        if (len > MAX_LEN) len = MAX_LEN;

        IntConsumer focus = this::focusSlot;

        int startX = calcSlotsStartX(len);
        int y = calcSlotsY();

        for (int i = 0; i < len; i++) {
            int x = startX + i * (slotWidth + slotGap);
            CharSlotWidget slot = new CharSlotWidget(x, y, slotWidth, slotHeight, i, focus);
            // подтянуть значение из state (если есть)
            slot.refreshFromState();
            addDrawableChild(slot);
            slots.add(slot);
        }

        focusFirstEmpty();
    }

    private int calcSlotsY() {
        return topPad + 10;
    }

    private int calcSlotsStartX(int len) {
        int totalW = len * slotWidth + (len - 1) * slotGap;
        return Math.max(controlsPad, (width - totalW) / 2);
    }

    private void focusFirstEmpty() {
        int len = PatternState.get().getLength();
        int firstEmpty = 0;
        for (int i = 0; i < len; i++) {
            if (PatternState.get().getChar(i) == '\0') { firstEmpty = i; break; }
        }
        focusSlot(firstEmpty);
    }

    private void focusSlot(int i) {
        if (slots.isEmpty()) return;
        if (i < 0) i = 0;
        if (i >= slots.size()) i = slots.size() - 1;
        setFocused(slots.get(i));
    }

    private void onLengthChanged(int newLen) {
        int clamped = Math.max(MIN_LEN, Math.min(MAX_LEN, newLen));
        if (clamped == PatternState.get().getLength()) return;
        PatternState.get().setLength(clamped);
        rebuildSlots();
        GTBHelper.updateSuggestionsAsync();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);

        // Заголовок и текущая длина
        String title = "GTB Solver";
        String lenText = "Длина: " + PatternState.get().getLength();

        var tr = MinecraftClient.getInstance().textRenderer;
        int titleW = tr.getWidth(title);
        ctx.drawText(tr, title, (width - titleW) / 2, 8, 0xFFFFFF, false);
        ctx.drawText(tr, lenText, controlsPad + 2, 8, 0xA0FFFFFF, false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        // пересобрать компоновку при смене размера
        this.init(client, width, height);
    }

    @Override
    public void onClose() {
        // состояние уже сохранено в PatternState по факту ввода — просто закрываем
        super.onClose();
        if (client != null) client.setScreen(null);
    }
}
