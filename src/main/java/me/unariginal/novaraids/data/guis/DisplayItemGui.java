package me.unariginal.novaraids.data.guis;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class DisplayItemGui extends BaseGuiData {
    public GuiButton displayButton;

    public DisplayItemGui(JsonObject guiObject,
                          String title,
                          int rows,
                          List<String> layout,
                          GuiButton backgroundButton,
                          GuiButton closeButton,
                          GuiButton nextButton,
                          GuiButton previousButton,
                          GuiButton displayButton) {
        super(guiObject, title, rows, layout, backgroundButton, closeButton, nextButton, previousButton);
        this.displayButton = displayButton;
    }

    public int displaySlotTotal() {
        int count = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == displayButton.symbol().charAt(0)) {
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
                if (c == displayButton.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }
}
