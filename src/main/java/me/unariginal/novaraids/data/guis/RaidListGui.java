package me.unariginal.novaraids.data.guis;

import com.google.gson.JsonObject;
import net.minecraft.component.ComponentChanges;

import java.util.ArrayList;
import java.util.List;

public class RaidListGui extends BaseGuiData {
    public String displaySymbol;
    public String displayName;
    public List<String> joinableLore;
    public List<String> requiresPassLore;
    public List<String> inProgressLore;
    public ComponentChanges displayData;

    public RaidListGui(JsonObject guiObject,
                       String title,
                       int rows,
                       List<String> layout,
                       GuiButton backgroundButton,
                       GuiButton closeButton,
                       GuiButton nextButton,
                       GuiButton previousButton,
                       String displaySymbol,
                       String displayName,
                       List<String> joinableLore,
                       List<String> requiresPassLore,
                       List<String> inProgressLore,
                       ComponentChanges displayData) {
        super(guiObject, title, rows, layout, backgroundButton, closeButton, nextButton, previousButton);
        this.displaySymbol = displaySymbol;
        this.displayName = displayName;
        this.joinableLore = joinableLore;
        this.requiresPassLore = requiresPassLore;
        this.inProgressLore = inProgressLore;
        this.displayData = displayData;
    }

    public int displaySlotTotal() {
        int count = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == displaySymbol.charAt(0)) {
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
                if (c == displaySymbol.charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }
}
