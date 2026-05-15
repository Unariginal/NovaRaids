package me.unariginal.novaraids.utils;

public class GuiUtils {
    public static int getPageTotal(int total_elements, int size) {
        return (int) Math.ceil((double) total_elements / size);
    }
}
