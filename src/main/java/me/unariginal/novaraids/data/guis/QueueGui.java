package me.unariginal.novaraids.data.guis;

import net.minecraft.component.ComponentChanges;

import java.util.ArrayList;
import java.util.List;

public class QueueGui extends BaseGuiData {
    public String display_symbol;
    public String display_name;
    public List<String> default_lore;
    public List<String> cancel_lore;
    public ComponentChanges display_data;

    public QueueGui(String title,
                    int rows,
                    List<String> layout,
                    GuiButton background_button,
                    GuiButton close_button,
                    GuiButton next_button,
                    GuiButton previous_button,
                    String display_symbol,
                    String display_name,
                    List<String> default_lore,
                    List<String> cancel_lore,
                    ComponentChanges display_data) {
        super(title, rows, layout, background_button, close_button, next_button, previous_button);
        this.display_symbol = display_symbol;
        this.display_name = display_name;
        this.default_lore = default_lore;
        this.cancel_lore = cancel_lore;
        this.display_data = display_data;
    }

    public int displaySlotTotal() {
        int count = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == display_symbol.charAt(0)) {
                    count++;
                }
            }
        }
        return count;
    }

    public List<Integer> displaySlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == display_symbol.charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }
}
