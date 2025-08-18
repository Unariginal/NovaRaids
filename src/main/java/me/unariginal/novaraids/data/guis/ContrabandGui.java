package me.unariginal.novaraids.data.guis;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ContrabandGui extends BaseGuiData {
    public boolean useHopperGui;
    public GuiButton bannedPokemonButton;
    public DisplayItemGui bannedPokemon;
    public GuiButton bannedMovesButton;
    public DisplayItemGui bannedMoves;
    public GuiButton bannedAbilitiesButton;
    public DisplayItemGui bannedAbilities;
    public GuiButton bannedHeldItemsButton;
    public DisplayItemGui bannedHeldItems;
    public GuiButton bannedBagItemsButton;
    public DisplayItemGui bannedBagItems;

    public ContrabandGui(JsonObject guiObject,
                         String title,
                         int rows,
                         List<String> layout,
                         GuiButton backgroundButton,
                         GuiButton closeButton,
                         GuiButton nextButton,
                         GuiButton previousButton,
                         boolean useHopperGui,
                         GuiButton bannedPokemonButton,
                         DisplayItemGui bannedPokemon,
                         GuiButton bannedMovesButton,
                         DisplayItemGui bannedMoves,
                         GuiButton bannedAbilitiesButton,
                         DisplayItemGui bannedAbilities,
                         GuiButton bannedHeldItemsButton,
                         DisplayItemGui bannedHeldItems,
                         GuiButton bannedBagItemsButton,
                         DisplayItemGui bannedBagItems) {
        super(guiObject, title, rows, layout, backgroundButton, closeButton, nextButton, previousButton);
        this.useHopperGui = useHopperGui;
        this.bannedPokemonButton = bannedPokemonButton;
        this.bannedPokemon = bannedPokemon;
        this.bannedMovesButton = bannedMovesButton;
        this.bannedMoves = bannedMoves;
        this.bannedAbilitiesButton = bannedAbilitiesButton;
        this.bannedAbilities = bannedAbilities;
        this.bannedHeldItemsButton = bannedHeldItemsButton;
        this.bannedHeldItems = bannedHeldItems;
        this.bannedBagItemsButton = bannedBagItemsButton;
        this.bannedBagItems = bannedBagItems;
    }

    public List<Integer> pokemonSlots() {
        List<Integer> slots = new ArrayList<>();
        int slot = 0;
        for (String line : layout) {
            for (char c : line.toCharArray()) {
                if (c == bannedPokemonButton.symbol().charAt(0)) {
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
                if (c == bannedMovesButton.symbol().charAt(0)) {
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
                if (c == bannedAbilitiesButton.symbol().charAt(0)) {
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
                if (c == bannedHeldItemsButton.symbol().charAt(0)) {
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
                if (c == bannedBagItemsButton.symbol().charAt(0)) {
                    slots.add(slot);
                }
                slot++;
            }
        }
        return slots;
    }
}
