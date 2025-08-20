package com.romoz.gtb.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;

import java.util.ArrayList;
import java.util.List;

public class SuggestionListWidget extends EntryListWidget<SuggestionListWidget.Entry> {

    public static SuggestionListWidget INSTANCE;

    private final List<String> items = new ArrayList<>();

    public SuggestionListWidget(MinecraftClient client, int width, int height, int top, int left) {
        super(client, width, height, top, 12);
        this.setX(left);
    }

    public void setItems(List<String> words) {
        items.clear();
        if (words != null) items.addAll(words);
        this.clearEntries();
        for (String w : items) this.addEntry(new Entry(w));
    }

    /** Требуется ClickableWidget-предком — добавим пустую озвучку. */
    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // no-op
    }

    public static class Entry extends EntryListWidget.Entry<Entry> {
        private final String value;

        public Entry(String v) { this.value = v; }

        @Override
        public void render(DrawContext ctx, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            var tr = MinecraftClient.getInstance().textRenderer;
            ctx.drawText(tr, value, x + 2, y + 2, 0xFFFFFF, false);
        }

        // не помечаем @Override, чтобы не упираться в различия мэппингов
        public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }

        public java.util.List<? extends net.minecraft.client.gui.Element> children() { return java.util.List.of(); }
    }
}
