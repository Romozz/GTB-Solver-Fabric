package com.romoz.gtb.ui;

import com.romoz.gtb.logic.CandidatesProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Оверлей для ввода длины/букв и просмотра кандидатов.
 * Открывается хоткеем из GTBSolver.
 */
public class GTBOverlayScreen extends Screen {

    private final PatternState state;

    private TextFieldWidget lengthField;
    private SuggestionListWidget suggestions;

    private long lastChangeNanos = 0L;                 // для дебаунса
    private static final long DEBOUNCE_NS = 75_000_000L; // 75 мс

    // параметры сетки слотов
    private static final int MAX_PER_ROW = 18;
    private static final int GAP = 4;
    private static final int CELL = 20;

    // координаты и размеры основного окна
    private int winLeft, winTop, winW, winH;

    // трекер динамически добавленных виджетов (чтобы корректно удалять)
    private final List<Object> dynamicWidgets = new ArrayList<>();

    public GTBOverlayScreen(PatternState state) {
        super(Text.translatable("screen.gtbsolver.title"));
        this.state = state;
    }

    @Override
    protected void init() {
        // размеры окна
        winW = 360;
        winH = 260;
        winLeft = (this.width - winW) / 2;
        winTop  = (this.height - winH) / 2;

        // ===== поле "Длина" =====
        lengthField = new TextFieldWidget(textRenderer, winLeft + 12, winTop + 12, 64, 20, Text.of("len"));
        lengthField.setPlaceholder(Text.of("len"));
        lengthField.setChangedListener(s -> {
            int len = parseLen(s);
            if (len != state.getLength()) {
                state.setLength(len);
                rebuildSlotsAndList();   // пересоздать сетку и список
                markDirty();             // запросить пересчёт кандидатов
            }
        });
        if (state.getLength() <= 0) state.setLength(10);
        lengthField.setText(Integer.toString(state.getLength()));
        addDrawableChild(lengthField);

        // ===== кнопка "Очистить" =====
        addDrawableChild(ButtonWidget.builder(Text.translatable("gtbsolver.clear"), b -> {
            state.clear();
            markDirty();
        }).dimensions(winLeft + 84, winTop + 12, 64, 20).build());

        // ===== кнопка "Вставить в чат" (без автосенда) =====
        addDrawableChild(ButtonWidget.builder(Text.translatable("gtbsolver.paste_to_chat"), b -> {
            String best = suggestions != null ? suggestions.getSelectedOrFirst() : null;
            if (best != null) insertToChat(best);
        }).dimensions(winLeft + 152, winTop + 12, 196, 20).build());

        // первичное создание слотов и списка
        rebuildSlotsAndList();

        // подтягиваем обновления от PatternState (например, из action bar)
        state.setListener(s -> {
            if (lengthField != null && s.getLength() != parseLen(lengthField.getText())) {
                lengthField.setText(Integer.toString(s.getLength()));
                rebuildSlotsAndList();
            }
            markDirty();
        });
    }

    private int parseLen(String s) {
        try { return Math.max(1, Math.min(64, Integer.parseInt(s.trim()))); }
        catch (Exception e) { return 1; }
    }

    private void insertToChat(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.keyboard.setClipboard(text);
            if (mc.inGameHud != null) {
                mc.inGameHud.getChatHud().addToMessageHistory(text);
            }
            mc.setScreen(null);
        }
    }

    private void clearDynamicWidgets() {
        // аккуратно удаляем ранее добавленные слоты/список через публичный API
        for (Object o : dynamicWidgets) {
            if (o instanceof SuggestionListWidget w) this.remove(w);
            else if (o instanceof CharSlotWidget w) this.remove(w);
        }
        dynamicWidgets.clear();
        suggestions = null;
    }

    private void rebuildSlotsAndList() {
        clearDynamicWidgets();

        // ===== сетка слотов =====
        int len = state.getLength();
        int x = winLeft + 12;
        int y = winTop + 44;

        // …в методе rebuildSlotsAndList():
        for (int i = 0; i < len; i++) {
            final int slotIndex = i; // фиксируем индекс для использования в лямбде

            int col = i % MAX_PER_ROW;
            int row = i / MAX_PER_ROW;
            int cx = x + col * (CELL + GAP);
            int cy = y + row * (CELL + GAP);

            CharSlotWidget slot = new CharSlotWidget(
                textRenderer, cx, cy, CELL, CELL, i,
                ch -> {
                    // теперь используем slotIndex, а не изменяемый i
                    state.setChar(slotIndex, ch);
                    markDirty();
                }
            );
            CharSlotWidget added = addDrawableChild(slot);
            dynamicWidgets.add(added);
        }


        // ===== список кандидатов =====
        int rows = (int)Math.ceil(len / (double)MAX_PER_ROW);
        int listLeft = winLeft + 12;
        int listTop  = y + rows * (CELL + GAP) + 8;
        int listW    = 336;
        int listH    = 120;

        suggestions = new SuggestionListWidget(client, listW, listH, listTop, listTop + listH);
        suggestions.setX(listLeft);
        SuggestionListWidget addedList = addDrawableChild(suggestions);
        dynamicWidgets.add(addedList);

        markDirty(); // пересчитать кандидатов с новым layout
    }

    private void markDirty() {
        lastChangeNanos = System.nanoTime();
    }

    @Override
    public void tick() {
        // Дебаунс пересчёта списка
        if (suggestions != null && System.nanoTime() - lastChangeNanos >= DEBOUNCE_NS) {
            String regex = state.toPatternRegex();
            int len = state.getLength();
            List<String> result = CandidatesProvider.find(regex, len);
            suggestions.setItems(result);
            lastChangeNanos = Long.MAX_VALUE; // ждать следующее изменение
        }
    }

    @Override
    public void render(DrawContext dc, int mouseX, int mouseY, float delta) {
        this.renderBackground(dc, mouseX, mouseY, delta);
        // фон окна
        dc.fill(winLeft, winTop, winLeft + winW, winTop + winH, 0xC0101010);
        super.render(dc, mouseX, mouseY, delta);
        // заголовки
        dc.drawText(textRenderer, Text.literal("GTB Solver"), winLeft + 12, winTop - 10, 0xFFFFFF, true);
        dc.drawText(textRenderer, Text.literal("Длина"),     winLeft + 12, winTop + 2,  0xAAAAAA, false);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Навигация по списку ↑/↓/Enter
        if (suggestions != null) {
            if (keyCode == GLFW.GLFW_KEY_UP) {
                suggestions.moveSelection(-1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                suggestions.moveSelection(+1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                String sel = suggestions.getSelectedOrFirst();
                if (sel != null) insertToChat(sel);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
