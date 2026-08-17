package me.unariginal.novaraids.config.guis;

import me.unariginal.novaraids.data.guis.BaseGUI;
import me.unariginal.novaraids.data.guis.BaseGUIItem;

import java.util.List;

public class LeaderboardGuiConfig extends BaseGUI {
    public BaseGUIItem placementItem;

    public LeaderboardGuiConfig(String guiTitle, Boolean useHopperGui, int rows, List<String> guiLayout, BaseGUIItem placementItem, BaseGUIItem backgroundItem, BaseGUIItem previousItem, BaseGUIItem nextItem, BaseGUIItem closeItem) {
        super(guiTitle, useHopperGui, rows, guiLayout, backgroundItem, previousItem, nextItem, closeItem);
        this.placementItem = placementItem;
    }
}
