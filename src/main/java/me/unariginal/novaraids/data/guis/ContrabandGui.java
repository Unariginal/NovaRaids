package me.unariginal.novaraids.data.guis;

import java.util.ArrayList;
import java.util.List;

public class ContrabandGui extends BaseGuiData {
    public boolean use_hopper_gui;
    public GuiButton banned_pokemon_button;
    public DisplayItemGui banned_pokemon;
    public GuiButton banned_moves_button;
    public DisplayItemGui banned_moves;
    public GuiButton banned_abilities_button;
    public DisplayItemGui banned_abilities;
    public GuiButton banned_held_items_button;
    public DisplayItemGui banned_held_items;
    public GuiButton banned_bag_items_button;
    public DisplayItemGui banned_bag_items;

    public ContrabandGui(String title,
                         int rows,
                         List<String> layout,
                         GuiButton background_button,
                         GuiButton close_button,
                         GuiButton next_button,
                         GuiButton previous_button,
                         boolean use_hopper_gui,
                         GuiButton banned_pokemon_button,
                         DisplayItemGui banned_pokemon,
                         GuiButton banned_moves_button,
                         DisplayItemGui banned_moves,
                         GuiButton banned_abilities_button,
                         DisplayItemGui banned_abilities,
                         GuiButton banned_held_items_button,
                         DisplayItemGui banned_held_items,
                         GuiButton banned_bag_items_button,
                         DisplayItemGui banned_bag_items) {
        super(title, rows, layout, background_button, close_button, next_button, previous_button);
        this.use_hopper_gui = use_hopper_gui;
        this.banned_pokemon_button = banned_pokemon_button;
        this.banned_pokemon = banned_pokemon;
        this.banned_moves_button = banned_moves_button;
        this.banned_moves = banned_moves;
        this.banned_abilities_button = banned_abilities_button;
        this.banned_abilities = banned_abilities;
        this.banned_held_items_button = banned_held_items_button;
        this.banned_held_items = banned_held_items;
        this.banned_bag_items_button = banned_bag_items_button;
        this.banned_bag_items = banned_bag_items;
    }

    public List<Integer> pokemonSlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == banned_pokemon_button.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }

    public List<Integer> moveSlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == banned_moves_button.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }

    public List<Integer> abilitySlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == banned_abilities_button.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }

    public List<Integer> heldItemSlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == banned_held_items_button.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }

    public List<Integer> bagItemSlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == banned_bag_items_button.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }
}
