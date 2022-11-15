package me.kamelajda.valorantrpc.utils;

import de.jcm.discordgamesdk.activity.Activity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.kamelajda.valorantrpc.dto.SocketData;
import me.kamelajda.valorantrpc.services.DiscordService;
import me.kamelajda.valorantrpc.services.LanguageService;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
@Getter
public abstract class PresenceState {

    private final DiscordService discordService;
    private final LanguageService languageService;

    public boolean tryApply(SocketData socketData, Activity activity) {
        throw new UnsupportedOperationException("implementation");
    }

    protected void applyAgentData(@Nullable Agent agent, Activity activity) {
        if (agent != null) {
            activity.assets().setSmallImage(agent.getKey());
            activity.assets().setSmallText(agent.getDisplayName());
        }
    }

    public String getTranslate(String key, Object... toReplace) {
        return languageService.get(key, toReplace);
    }

    public String getTranslate(String key) {
        return languageService.get(key);
    }

}
