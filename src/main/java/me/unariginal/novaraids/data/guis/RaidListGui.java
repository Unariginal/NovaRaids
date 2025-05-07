package me.unariginal.novaraids.data.guis;

import net.minecraft.component.ComponentChanges;

import java.util.ArrayList;
import java.util.List;

public class RaidListGui extends BaseGuiData {
    public String display_symbol;
    public String display_name;
    public List<String> joinable_lore;
    public List<String> requires_pass_lore;
    public List<String> in_progress_lore;
    public ComponentChanges display_data;

    public RaidListGui(String title,
                       int rows,
                       List<String> layout,
                       GuiButton background_button,
                       GuiButton close_button,
                       GuiButton next_button,
                       GuiButton previous_button,
                       String display_symbol,
                       String display_name,
                       List<String> joinable_lore,
                       List<String> requires_pass_lore,
                       List<String> in_progress_lore,
                       ComponentChanges display_data) {
        super(title, rows, layout, background_button, close_button, next_button, previous_button);
        this.display_symbol = display_symbol;
        this.display_name = display_name;
        this.joinable_lore = joinable_lore;
        this.requires_pass_lore = requires_pass_lore;
        this.in_progress_lore = in_progress_lore;
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
