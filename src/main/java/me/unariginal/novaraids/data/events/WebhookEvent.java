package me.unariginal.novaraids.data.events;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WebhookEvent {
    public String message;
    public String embedTitle;
    public List<EmbedField> fields;
    @Nullable
    public EmbedField leaderboardFieldLayout;

    public static class EmbedField {
        public boolean inline;
        public String name;
        public String value;
        @Nullable
        public Boolean insertLeaderboardAfter;
    }
}
