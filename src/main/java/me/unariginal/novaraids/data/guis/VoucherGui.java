package me.unariginal.novaraids.data.guis;

import net.minecraft.component.ComponentChanges;

import java.util.ArrayList;
import java.util.List;

public class VoucherGui extends BaseGuiData {
    public String display_symbol;
    public String display_name;
    public List<String> display_lore;
    public ComponentChanges display_data;

    public VoucherGui(String title,
                      int rows,
                      List<String> layout,
                      GuiButton background_button,
                      GuiButton close_button,
                      GuiButton next_button,
                      GuiButton previous_button,
                      String display_symbol,
                      String display_name,
                      List<String> display_lore,
                      ComponentChanges display_data) {
        super(title, rows, layout, background_button, close_button, next_button, previous_button);
        this.display_symbol = display_symbol;
        this.display_name = display_name;
        this.display_lore = display_lore;
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
