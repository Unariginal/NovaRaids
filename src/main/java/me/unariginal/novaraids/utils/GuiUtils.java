package me.unariginal.novaraids.utils;

import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public class GuiUtils {
    public static ScreenHandlerType<GenericContainerScreenHandler> getScreenSize(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }

    public static int getPageTotal(int total_elements, int size) {
        return (int) Math.ceil((double) total_elements / size);
    }

    public static int getPage(int index, int size) {
        return (int) Math.ceil((double) index / size);
    }

    public static int getSlotOnPage(int index, int size) {
        return (index % size) - 1;
    }
}
