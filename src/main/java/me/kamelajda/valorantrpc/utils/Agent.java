package me.kamelajda.valorantrpc.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
@Getter
public enum Agent {

    ASTRA("Astra"),
    BREACH("Breach"),
    BRIMSTONE("Brimstone"),
    CHAMBER("Chamber"),
    CYPHER("Cypher"),
    FADE("Fade"),
    HARBOR("Harbor"),
    JETT("Jett"),
    KAYO("KAY/O"),
    KILLJOY("Killjoy"),
    NEON("Neon"),
    OMEN("Omen"),
    PHOENIX("Phoenix"),
    RAZE("Raze"),
    REYNA("Reyna"),
    SAGE("Sage"),
    SKYE("Skye"),
    SOVA("Sova"),
    VIPER("Viper"),
    YORU("Yoru")
    ;

    private final String displayName;

    public String getKey() {
        return "agent_" + this.displayName.toLowerCase().replaceAll("/", "");
    }

    public String getTranslateKey() {
        return "utils.agent." + name().toLowerCase();
    }

    @Nullable
    public static Agent findForKey(String key) {
        for (Agent agent : values()) {
            if (agent.getDisplayName().equalsIgnoreCase(key)) return agent;
        }
        log.error("Agent for {} not found!", key);
        return null;
    }

}
