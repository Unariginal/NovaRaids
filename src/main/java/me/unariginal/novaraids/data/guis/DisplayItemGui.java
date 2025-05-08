package me.unariginal.novaraids.data.guis;

import java.util.ArrayList;
import java.util.List;

public class DisplayItemGui extends BaseGuiData {
    public GuiButton display_button;

    public DisplayItemGui(String title,
                          int rows,
                          List<String> layout,
                          GuiButton background_button,
                          GuiButton close_button,
                          GuiButton next_button,
                          GuiButton previous_button,
                          GuiButton display_button) {
        super(title, rows, layout, background_button, close_button, next_button, previous_button);
        this.display_button = display_button;
    }

    public int displaySlotTotal() {
        int count = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == display_button.symbol().charAt(0)) {
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
                if (c == display_button.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }
}
