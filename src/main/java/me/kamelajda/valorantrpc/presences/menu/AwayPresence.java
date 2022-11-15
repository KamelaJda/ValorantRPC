package me.kamelajda.valorantrpc.presences.menu;

import de.jcm.discordgamesdk.activity.Activity;
import me.kamelajda.valorantrpc.dto.SocketData;
import me.kamelajda.valorantrpc.services.DiscordService;
import me.kamelajda.valorantrpc.services.LanguageService;
import me.kamelajda.valorantrpc.utils.PresenceImages;
import me.kamelajda.valorantrpc.utils.PresenceState;

public class AwayPresence extends PresenceState {

    public AwayPresence(DiscordService discordService, LanguageService languageService) {
        super(discordService, languageService);
    }

    @Override
    public boolean tryApply(SocketData socketData, Activity activity) {
        if (!socketData.getStateDetails().isIdle()) return false;

        String type = socketData.getStateDetails().getSessionLoopState().equals("MENUS")
            ? getTranslate("presences.in_menu.in_menu") : getTranslate("presences.away.in_game");

        activity.setDetails(getTranslate("presences.away.idle"));
        activity.setState(type);

        activity.assets().setSmallImage(PresenceImages.IDLE.getKey());
        activity.assets().setSmallText(getTranslate("presences.away.idle"));

        return true;
    }

}
