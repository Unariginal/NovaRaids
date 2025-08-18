package me.unariginal.novaraids.data.guis;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class BaseGuiData {
    public JsonObject guiObject;
    public String title;
    public int rows;
    public List<String> layout;
    public GuiButton backgroundButton;
    public GuiButton closeButton;
    public GuiButton nextButton;
    public GuiButton previousButton;

    public BaseGuiData(JsonObject guiObject, String title, int rows, List<String> layout, GuiButton backgroundButton, GuiButton closeButton, GuiButton nextButton, GuiButton previousButton) {
        this.guiObject = guiObject;
        this.title = title;
        this.rows = rows;
        this.layout = layout;
        this.backgroundButton = backgroundButton;
        this.closeButton = closeButton;
        this.nextButton = nextButton;
        this.previousButton = previousButton;
    }

    public List<Integer> nextButtonSlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == nextButton.symbol().charAt(0)) {
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
                if (c == backgroundButton.symbol().charAt(0)) {
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
                if (c == previousButton.symbol().charAt(0)) {
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
                if (c == closeButton.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }
}
