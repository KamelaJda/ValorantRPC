package me.kamelajda.valorantrpc.presences;

import de.jcm.discordgamesdk.activity.Activity;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.dto.SocketData;
import me.kamelajda.valorantrpc.services.DiscordService;
import me.kamelajda.valorantrpc.services.LanguageService;
import me.kamelajda.valorantrpc.utils.PresenceState;

@Slf4j
public class StartingPresence extends PresenceState {

    public StartingPresence(DiscordService discordService, LanguageService languageService) {
        super(discordService, languageService);
    }

    @Override
    public boolean tryApply(SocketData socketData, Activity activity) {
        if (socketData != null) return false;

        activity.setDetails(getTranslate("presences.starting.in_game"));

        getDiscordService().updateRP(activity);
        return true;
    }

}
