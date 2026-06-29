package me.unariginal.novaraids.config.guis;

import me.unariginal.novaraids.data.guis.BaseGUI;
import me.unariginal.novaraids.data.guis.BaseGUIItem;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ContrabandGuiConfig extends BaseGUI {
    public PokemonSubGUIItem bannedPokemon;
    public MovesSubGUIItem bannedMoves;
    public AbilitiesSubGUIItem bannedAbilities;
    public HeldItemsSubGUIItem bannedHeldItems;
    public BagItemsSubGUIItem bannedBagItems;

    public static class PokemonSubGUIItem extends BaseGUIItem {
        public PokemonSubGUI guiSettings;

        public static class PokemonSubGUI extends BaseGUI {
            public BaseGUIItem pokemonDisplayItem;

            public PokemonSubGUI(String guiTitle, Boolean useHopperGui, int rows, List<String> guiLayout, BaseGUIItem pokemonDisplayItem, BaseGUIItem backgroundItem, BaseGUIItem previousItem, BaseGUIItem nextItem, BaseGUIItem closeItem) {
                super(guiTitle, useHopperGui, rows, guiLayout, backgroundItem, previousItem, nextItem, closeItem);
                this.pokemonDisplayItem = pokemonDisplayItem;
            }
        }

        public PokemonSubGUIItem(String symbol, ItemStack item, String itemName, List<String> itemLore, PokemonSubGUI guiSettings) {
            super(symbol, item, itemName, itemLore);
            this.guiSettings = guiSettings;
        }
    }

    public static class MovesSubGUIItem extends BaseGUIItem {
        public MovesSubGUI guiSettings;

        public static class MovesSubGUI extends BaseGUI {
            public BaseGUIItem moveDisplayItem;

            public MovesSubGUI(String guiTitle, Boolean useHopperGui, int rows, List<String> guiLayout, BaseGUIItem moveDisplayItem, BaseGUIItem backgroundItem, BaseGUIItem previousItem, BaseGUIItem nextItem, BaseGUIItem closeItem) {
                super(guiTitle, useHopperGui, rows, guiLayout, backgroundItem, previousItem, nextItem, closeItem);
                this.moveDisplayItem = moveDisplayItem;
            }
        }

        public MovesSubGUIItem(String symbol, ItemStack item, String itemName, List<String> itemLore, MovesSubGUI guiSettings) {
            super(symbol, item, itemName, itemLore);
            this.guiSettings = guiSettings;
        }
    }

    public static class AbilitiesSubGUIItem extends BaseGUIItem {
        public AbilitiesSubGUI guiSettings;

        public static class AbilitiesSubGUI extends BaseGUI {
            public BaseGUIItem abilityDisplayItem;

            public AbilitiesSubGUI(String guiTitle, Boolean useHopperGui, int rows, List<String> guiLayout, BaseGUIItem abilityDisplayItem, BaseGUIItem backgroundItem, BaseGUIItem previousItem, BaseGUIItem nextItem, BaseGUIItem closeItem) {
                super(guiTitle, useHopperGui, rows, guiLayout, backgroundItem, previousItem, nextItem, closeItem);
                this.abilityDisplayItem = abilityDisplayItem;
            }
        }

        public AbilitiesSubGUIItem(String symbol, ItemStack item, String itemName, List<String> itemLore, AbilitiesSubGUI guiSettings) {
            super(symbol, item, itemName, itemLore);
            this.guiSettings = guiSettings;
        }
    }

    public static class HeldItemsSubGUIItem extends BaseGUIItem {
        public HeldItemsSubGUI guiSettings;

        public static class HeldItemsSubGUI extends BaseGUI {
            public BaseGUIItem heldItemDisplayItem;

            public HeldItemsSubGUI(String guiTitle, Boolean useHopperGui, int rows, List<String> guiLayout, BaseGUIItem heldItemDisplayItem, BaseGUIItem backgroundItem, BaseGUIItem previousItem, BaseGUIItem nextItem, BaseGUIItem closeItem) {
                super(guiTitle, useHopperGui, rows, guiLayout, backgroundItem, previousItem, nextItem, closeItem);
                this.heldItemDisplayItem = heldItemDisplayItem;
            }
        }

        public HeldItemsSubGUIItem(String symbol, ItemStack item, String itemName, List<String> itemLore, HeldItemsSubGUI guiSettings) {
            super(symbol, item, itemName, itemLore);
            this.guiSettings = guiSettings;
        }
    }

    public static class BagItemsSubGUIItem extends BaseGUIItem {
        public BagItemsSubGUI guiSettings;

        public static class BagItemsSubGUI extends BaseGUI {
            public BaseGUIItem bagItemDisplayItem;

            public BagItemsSubGUI(String guiTitle, Boolean useHopperGui, int rows, List<String> guiLayout, BaseGUIItem bagItemDisplayItem, BaseGUIItem backgroundItem, BaseGUIItem previousItem, BaseGUIItem nextItem, BaseGUIItem closeItem) {
                super(guiTitle, useHopperGui, rows, guiLayout, backgroundItem, previousItem, nextItem, closeItem);
                this.bagItemDisplayItem = bagItemDisplayItem;
            }
        }

        public BagItemsSubGUIItem(String symbol, ItemStack item, String itemName, List<String> itemLore, BagItemsSubGUI guiSettings) {
            super(symbol, item, itemName, itemLore);
            this.guiSettings = guiSettings;
        }
    }

    public ContrabandGuiConfig(
            String guiTitle,
            Boolean useHopperGui,
            int rows,
            List<String> guiLayout,
            PokemonSubGUIItem bannedPokemon,
            MovesSubGUIItem bannedMoves,
            AbilitiesSubGUIItem bannedAbilities,
            HeldItemsSubGUIItem bannedHeldItems,
            BagItemsSubGUIItem bannedBagItems,
            BaseGUIItem backgroundItem,
            BaseGUIItem previousItem,
            BaseGUIItem nextItem,
            BaseGUIItem closeItem
    ) {
        super(guiTitle, useHopperGui, rows, guiLayout, backgroundItem, previousItem, nextItem, closeItem);
        this.bannedPokemon = bannedPokemon;
        this.bannedMoves = bannedMoves;
        this.bannedAbilities = bannedAbilities;
        this.bannedHeldItems = bannedHeldItems;
        this.bannedBagItems = bannedBagItems;
    }
}
