package me.unariginal.novaraids.config.guis;

import me.unariginal.novaraids.data.guis.BaseGUI;
import me.unariginal.novaraids.data.guis.BaseGUIItem;

import java.util.List;

public class RaidItemGuiConfig extends BaseGUI {
    public BaseGUIItem raidDisplayItem;

    public RaidItemGuiConfig(
            String guiTitle,
            Boolean useHopperGui,
            int rows,
            List<String> guiLayout,
            BaseGUIItem raidDisplayItem,
            BaseGUIItem backgroundItem,
            BaseGUIItem previousItem,
            BaseGUIItem nextItem,
            BaseGUIItem closeItem
    ) {
        super(guiTitle, useHopperGui, rows, guiLayout, backgroundItem, previousItem, nextItem, closeItem);
        this.raidDisplayItem = raidDisplayItem;
    }
}
