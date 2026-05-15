package me.unariginal.novaraids.data;

import com.google.gson.annotations.SerializedName;

public class BossbarSelection {
    public String setup;
    public String fight;
    public String preCatch;
    @SerializedName("catch")
    public String catchPhase;
}
