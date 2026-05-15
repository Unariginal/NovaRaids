package me.unariginal.novaraids.data.guis;

import net.minecraft.screen.ScreenHandlerType;

import java.util.ArrayList;
import java.util.List;

public class BaseGUI {
    public String guiTitle;
    public Boolean useHopperGui;
    public int rows;
    public List<String> guiLayout;
    public BaseGUIItem backgroundItem;
    public BaseGUIItem previousItem;
    public BaseGUIItem nextItem;
    public BaseGUIItem closeItem;

    public BaseGUI(
            String guiTitle,
            Boolean useHopperGui,
            int rows,
            List<String> guiLayout,
            BaseGUIItem backgroundItem,
            BaseGUIItem previousItem,
            BaseGUIItem nextItem,
            BaseGUIItem closeItem
    ) {
        this.guiTitle = guiTitle;
        this.useHopperGui = useHopperGui;
        this.rows = rows;
        this.guiLayout = guiLayout;
        this.backgroundItem = backgroundItem;
        this.previousItem = previousItem;
        this.nextItem = nextItem;
        this.closeItem = closeItem;
    }

    public ScreenHandlerType<?> getScreenHandler() {
        if (useHopperGui) return ScreenHandlerType.HOPPER;
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }

    public List<Integer> getSlotsBySymbol(String symbol) {
        List<Integer> returnSlots = new ArrayList<>();
        int slotCount = 0;
        for (String row : guiLayout) {
            for (char slot : row.toCharArray()) {
                if (slot == symbol.charAt(0)) {
                    returnSlots.add(slotCount);
                }
                slotCount++;
            }
        }
        return returnSlots;
    }

    public int getTotalSlotsBySymbol(String symbol) {
        int count = 0;
        for (String row : guiLayout) {
            for (char slot : row.toCharArray()) {
                if (slot == symbol.charAt(0)) {
                    count++;
                }
            }
        }
        return count;
    }
}
