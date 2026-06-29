package me.unariginal.novaraids.config.guis;

import me.unariginal.novaraids.data.guis.BaseGUI;
import me.unariginal.novaraids.data.guis.BaseGUIItem;

import java.util.List;

public class RaidListGuiConfig extends BaseGUI {
    public BaseGUIItem joinableRaidItem;
    public BaseGUIItem passRequiredRaidItem;
    public BaseGUIItem inProgressRaidItem;

    public RaidListGuiConfig(
            String guiTitle,
            Boolean useHopperGui,
            int rows,
            List<String> guiLayout,
            BaseGUIItem joinableRaidItem,
            BaseGUIItem passRequiredRaidItem,
            BaseGUIItem inProgressRaidItem,
            BaseGUIItem backgroundItem,
            BaseGUIItem previousItem,
            BaseGUIItem nextItem,
            BaseGUIItem closeItem
    ) {
        super(guiTitle, useHopperGui, rows, guiLayout, backgroundItem, previousItem, nextItem, closeItem);
        this.joinableRaidItem = joinableRaidItem;
        this.passRequiredRaidItem = passRequiredRaidItem;
        this.inProgressRaidItem = inProgressRaidItem;
    }
}
