package me.unariginal.novaraids.config.guis;

import me.unariginal.novaraids.data.guis.BaseGUI;
import me.unariginal.novaraids.data.guis.BaseGUIItem;

import java.util.List;

public class RaidQueueGuiConfig extends BaseGUI {
    public BaseGUIItem raidItem;
    public BaseGUIItem cancelableRaidItem;

    public RaidQueueGuiConfig(String guiTitle, Boolean useHopperGui, int rows, List<String> guiLayout, BaseGUIItem raidItem, BaseGUIItem cancelableRaidItem, BaseGUIItem backgroundItem, BaseGUIItem previousItem, BaseGUIItem nextItem, BaseGUIItem closeItem) {
        super(guiTitle, useHopperGui, rows, guiLayout, backgroundItem, previousItem, nextItem, closeItem);
        this.raidItem = raidItem;
        this.cancelableRaidItem = cancelableRaidItem;
    }
}
