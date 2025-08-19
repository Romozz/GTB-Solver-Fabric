package com.romoz.gtb.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class SuggestionListWidget extends AlwaysSelectedEntryListWidget<SuggestionListWidget.Entry> {

    private final List<String> items = new ArrayList<>();

    public SuggestionListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
        super(client, width, height, top, bottom, itemHeight);
    }

    public void setItems(List<String> newItems) {
        this.items.clear();
        if (newItems != null) this.items.addAll(newItems);
        this.clearEntries();
        for (String s : items) this.addEntry(new Entry(s));
        if (!this.children().isEmpty()) this.setSelected(this.children().get(0));
    }

    public void moveSelection(int delta) {
        int idx = this.getSelected() == null ? 0 : this.children().indexOf(this.getSelected());
        int n = this.children().size();
        if (n == 0) return;
        idx = Math.max(0, Math.min(n - 1, idx + delta));
        this.setSelected(this.children().get(idx));
    }

    public String getSelectedOrFirst() {
        Entry e = this.getSelected();
        if (e != null) return e.value;
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    protected int getScrollbarPositionX() {
        return this.getRowLeft() + this.getRowWidth() + 6;
    }

    @Override
    public int getRowWidth() {
        return this.width - 8;
    }

    @Override
    protected void renderList(DrawContext dc, int mouseX, int mouseY, float delta) {
        super.renderList(dc, mouseX, mouseY, delta);
        // Заголовок со счётчиком
        String caption = "Совпадения: " + items.size();
        dc.drawText(this.client.textRenderer, Text.literal(caption), this.getRowLeft(), this.getTop() - 10, 0xAAAAAA, false);
    }

    public class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        private final String value;

        public Entry(String v) { this.value = v; }

        @Override
        public void render(DrawContext dc, int idx, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
            int color = hovered || SuggestionListWidget.this.getSelected() == this ? 0xFFFFFFFF : 0xFFCCCCCC;
            dc.drawText(SuggestionListWidget.this.client.textRenderer, Text.literal(value), x + 4, y + 2, color, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            SuggestionListWidget.this.setSelected(this);
            // ЛКМ — оставить выбранным; вставка делается по кнопке/Enter
            return true;
        }
    }
}
