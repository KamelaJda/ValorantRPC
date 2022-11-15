package me.kamelajda.valorantrpc.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum GameType {

    UNKNOWN("unknown"),

    // Unrated
    UNRATED("unrated"),

    // Ranked
    COMPETITIVE("competitive"),

    // DM
    DEATHMATCH("deathmatch"),

    // Escalation
    GG_TEAM("ggteam"),

    // Spike Rush
    SPIKERUSH("spikerush"),

    // Replication
    ONEFA("onefa"),

    // Snowball Fight
    SNOWBALL("snowball"),

    // Custom game
    CUSTOM(""),
    ;

    private final String key;

    public String getTranslateKey() {
        return "utils.game_type." + this.name().toLowerCase();
    }

    public static GameType fromKey(String queueId) {
        for (GameType value : values()) {
            if (value.getKey().equals(queueId)) return value;
        }
        return UNKNOWN;
    }
}
