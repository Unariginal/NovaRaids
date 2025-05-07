package me.unariginal.novaraids.data.guis;

import java.util.ArrayList;
import java.util.List;

public class BaseGuiData {
    public String title;
    public int rows;
    public List<String> layout;
    public GuiButton background_button;
    public GuiButton close_button;
    public GuiButton next_button;
    public GuiButton previous_button;

    public BaseGuiData(String title, int rows, List<String> layout, GuiButton background_button, GuiButton close_button, GuiButton next_button, GuiButton previous_button) {
        this.title = title;
        this.rows = rows;
        this.layout = layout;
        this.background_button = background_button;
        this.close_button = close_button;
        this.next_button = next_button;
        this.previous_button = previous_button;
    }

    public List<Integer> nextButtonSlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == next_button.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }

    public List<Integer> backgroundButtonSlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == background_button.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }

    public List<Integer> previousButtonSlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == previous_button.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }

    public List<Integer> closeButtonSlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == close_button.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }
}
