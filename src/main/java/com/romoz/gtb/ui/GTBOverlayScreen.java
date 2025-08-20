package com.romoz.gtb.ui;

import com.romoz.gtb.GTBHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class GTBOverlayScreen extends Screen {

    private static final int MIN_LEN = 1;
    private static final int MAX_LEN = 25;

    private final List<CharSlotWidget> slots = new ArrayList<>();

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
        this.clearChildren();
        slots.clear();

        int btnY = topPad - 24;
        int btnW = 20;
        int btnH = 20;

        minusBtn = ButtonWidget.builder(Text.literal("−"), b -> onLengthChanged(PatternState.get().getLength() - 1))
                .dimensions(controlsPad, btnY, btnW, btnH).build();
        plusBtn = ButtonWidget.builder(Text.literal("+"), b -> onLengthChanged(PatternState.get().getLength() + 1))
                .dimensions(controlsPad + btnW + 4, btnY, btnW, btnH).build();

        clearBtn = ButtonWidget.builder(Text.literal("Очистить"), b -> {
                    PatternState.get().clearAll();
                    rebuildSlots();
                    focusFirstEmpty();
                    GTBHelper.updateSuggestionsAsync();
                })
                .dimensions(controlsPad + (btnW + 4) * 2 + 6, btnY, 70, btnH).build();

        closeBtn = ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(width - controlsPad - 72, btnY, 72, btnH).build();

        addDrawableChild(minusBtn);
        addDrawableChild(plusBtn);
        addDrawableChild(clearBtn);
        addDrawableChild(closeBtn);

        rebuildSlots();

        // твой SuggestionListWidget, судя по ошибке, имеет одну из сигнатур:
        // (MinecraftClient,int,int,int,int) — используем её
        int listTop = calcSlotsY() + slotHeight + 14;
        int listHeight = Math.max(40, height - listTop - 10);
        SuggestionListWidget.INSTANCE = new SuggestionListWidget(client, width - controlsPad * 2, listHeight,
                listTop, controlsPad);
        addSelectableChild(SuggestionListWidget.INSTANCE);

        GTBHelper.updateSuggestionsAsync();
    }

    private void rebuildSlots() {
        // перестраиваем весь экран, чтобы не лезть в приватные списки Screen
        int len = Math.max(MIN_LEN, Math.min(MAX_LEN, PatternState.get().getLength()));
        IntConsumer focus = this::focusSlot;

        int startX = calcSlotsStartX(len);
        int y = calcSlotsY();

        for (int i = 0; i < len; i++) {
            int x = startX + i * (slotWidth + slotGap);
            CharSlotWidget slot = new CharSlotWidget(x, y, slotWidth, slotHeight, i, focus);
            slot.refreshFromState();
            addDrawableChild(slot);
            slots.add(slot);
        }

        focusFirstEmpty();
    }

    private int calcSlotsY() { return topPad + 10; }

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
        // просто реинициализируем весь экран
        this.init(MinecraftClient.getInstance(), this.width, this.height);
        GTBHelper.updateSuggestionsAsync();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        var tr = MinecraftClient.getInstance().textRenderer;
        String title = "GTB Solver";
        String lenText = "Длина: " + PatternState.get().getLength();
        int titleW = tr.getWidth(title);
        ctx.drawText(tr, title, (width - titleW) / 2, 8, 0xFFFFFF, false);
        ctx.drawText(tr, lenText, controlsPad + 2, 8, 0xA0FFFFFF, false);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void close() {
        if (client != null) client.setScreen(null);
    }
}
