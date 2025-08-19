package com.romoz.gtb.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import com.romoz.gtb.logic.CandidatesProvider;

public class GTBOverlayScreen extends Screen {

    private final PatternState state;
    private TextFieldWidget lengthField;
    private final List<CharSlotWidget> slots = new ArrayList<>();
    private SuggestionListWidget suggestions;

    private long lastChangeNanos = 0L; // дебаунс
    private static final long DEBOUNCE_NS = 75_000_000L; // 75 мс

    public GTBOverlayScreen(PatternState state) {
        super(Text.translatable("screen.gtbsolver.title"));
        this.state = state;
    }

    @Override
    protected void init() {
        int w = 360;
        int h = 240;
        int left = (this.width - w) / 2;
        int top  = (this.height - h) / 2;

        // Поле длины
        lengthField = new TextFieldWidget(textRenderer, left + 12, top + 12, 64, 20, Text.of("len"));
        lengthField.setPlaceholder(Text.of("len"));
        lengthField.setChangedListener(s -> {
            int len = parseLen(s);
            if (len != state.getLength()) {
                state.setLength(len);
                rebuildSlots(left, top);
                markDirty();
            }
        });
        if (state.getLength() <= 0) state.setLength(10);
        lengthField.setText(Integer.toString(state.getLength()));
        addDrawableChild(lengthField);

        // Кнопки
        addDrawableChild(ButtonWidget.builder(Text.translatable("gtbsolver.clear"), b -> {
            state.clear();
            markDirty();
        }).dimensions(left + 84, top + 12, 64, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gtbsolver.paste_to_chat"), b -> {
            String best = suggestions != null ? suggestions.getSelectedOrFirst() : null;
            if (best != null) insertToChat(best);
        }).dimensions(left + 152, top + 12, 196, 20).build());

        // Слоты + список
        rebuildSlots(left, top);

        // Слушатель состояния от миксина (action bar)
        state.setListener(s -> {
            if (lengthField != null && s.getLength() != parseLen(lengthField.getText())) {
                lengthField.setText(Integer.toString(s.getLength()));
                rebuildSlots(left, top);
            }
            // обновление кандидатов
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
            mc.inGameHud.getChatHud().addToMessageHistory(text);
            mc.setScreen(null);
            mc.keyboard.setClipboard(text);
            // Просто вставка в поле чата (без автосенда)
            mc.player.sendMessage(Text.literal("§7[GTB] Вставлено в буфер, нажмите T и §fCtrl+V§7"), false);
        }
    }

    private void rebuildSlots(int left, int top) {
        // удалить старые
        slots.forEach(this::remove);
        if (suggestions != null) remove(suggestions);
        slots.clear();

        int maxPerRow = 18;
        int gap = 4;
        int cellW = 20;
        int len = state.getLength();
        int x = left + 12;
        int y = top + 44;

        for (int i = 0; i < len; i++) {
            int col = i % maxPerRow;
            int row = i / maxPerRow;
            int cx = x + col * (cellW + gap);
            int cy = y + row * (cellW + gap);

            CharSlotWidget slot = new CharSlotWidget(textRenderer, cx, cy, cellW, cellW, i, ch -> {
                state.setChar(i, ch);
                markDirty();
            });
            addDrawableChild(slot);
            slots.add(slot);
        }

        // список кандидатов
        int listLeft = left + 12;
        int rows = (int)Math.ceil(len / (double)maxPerRow);
        int listTop = y + rows * (cellW + gap) + 8;
        int listW = 336;
        int listH = 120;

        suggestions = new SuggestionListWidget(client, listW, listH, listTop, listTop + listH, 16);
        suggestions.setLeftPos(listLeft);
        addSelectableChild(suggestions);
        addDrawableChild(suggestions);

        markDirty();
    }

    private void markDirty() {
        lastChangeNanos = System.nanoTime();
    }

    @Override
    public void tick() {
        // Дебаунс пересчёта
        if (System.nanoTime() - lastChangeNanos >= DEBOUNCE_NS && suggestions != null) {
            String regex = state.toPatternRegex();
            int len = state.getLength();
            List<String> result = CandidatesProvider.find(regex, len);
            suggestions.setItems(result);
            lastChangeNanos = Long.MAX_VALUE;
        }
    }

    @Override
    public void render(DrawContext dc, int mouseX, int mouseY, float delta) {
        this.renderBackground(dc);
        int w = 360;
        int h = 240;
        int left = (this.width - w) / 2;
        int top  = (this.height - h) / 2;
        dc.fill(left, top, left + w, top + h, 0xC0101010);

        super.render(dc, mouseX, mouseY, delta);

        dc.drawText(textRenderer, Text.literal("GTB Solver"), left + 12, top - 10, 0xFFFFFF, true);
        dc.drawText(textRenderer, Text.literal("Длина"), left + 12, top + 2, 0xAAAAAA, false);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Навигация по списку ↑/↓/Enter
        if (suggestions != null) {
            if (keyCode == InputUtil.GLFW_KEY_UP) { suggestions.moveSelection(-1); return true; }
            if (keyCode == InputUtil.GLFW_KEY_DOWN) { suggestions.moveSelection(+1); return true; }
            if (keyCode == InputUtil.GLFW_KEY_ENTER) {
                String sel = suggestions.getSelectedOrFirst();
                if (sel != null) insertToChat(sel);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
