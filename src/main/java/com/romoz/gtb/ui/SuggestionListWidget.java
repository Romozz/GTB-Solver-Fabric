package com.romoz.gtb.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class SuggestionListWidget extends AlwaysSelectedEntryListWidget<SuggestionListWidget.Entry> {

    private final List<String> items = new ArrayList<>();
    public int getCount() { return items.size(); }
    
    public SuggestionListWidget(MinecraftClient client, int width, int height, int top, int bottom) {
        super(client, width, height, top, bottom);
    }

    public void setItems(List<String> newItems) {
        this.items.clear();
        if (newItems != null) this.items.addAll(newItems);
        this.clearEntries();
        for (String s : items) this.addEntry(new Entry(s));
        if (!this.children().isEmpty()) this.setSelected(this.children().get(0));
    }

    public void moveSelection(int delta) {
        Entry sel = this.getSelectedOrNull();
        int idx = (sel == null) ? -1 : this.children().indexOf(sel);
        int n = this.children().size();
        if (n == 0) return;
        int target = Math.max(0, Math.min(n - 1, (idx < 0 ? 0 : idx + delta)));
        this.setSelected(this.children().get(target));
        this.ensureVisible(this.getSelectedOrNull());
    }

    public String getSelectedOrFirst() {
        Entry e = this.getSelectedOrNull();
        if (e != null) return e.value;
        return items.isEmpty() ? null : items.get(0);
    }

    // В некоторых маппингах эти методы не помечены как @Override — оставим без аннотации.
    protected int getScrollbarPositionX() {
        return this.getRowLeft() + this.getRowWidth() + 6;
    }

    public int getRowWidth() {
        return this.width - 8;
    }

    public class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        private final String value;

        public Entry(String v) { this.value = v; }

        @Override
        public void render(DrawContext dc, int idx, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            boolean selected = SuggestionListWidget.this.getSelectedOrNull() == this;
            int color = (hovered || selected) ? 0xFFFFFFFF : 0xFFCCCCCC;
            dc.drawText(SuggestionListWidget.this.client.textRenderer, Text.literal(value), x + 4, y + 2, color, false);
        }

        @Override
        public Text getNarration() {
            return Text.literal("Suggestion " + value);
        }
    }
}
