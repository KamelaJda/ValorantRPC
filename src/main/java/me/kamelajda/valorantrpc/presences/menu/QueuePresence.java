package me.kamelajda.valorantrpc.presences.menu;

import de.jcm.discordgamesdk.activity.Activity;
import me.kamelajda.valorantrpc.dto.SocketData;
import me.kamelajda.valorantrpc.services.DiscordService;
import me.kamelajda.valorantrpc.services.LanguageService;
import me.kamelajda.valorantrpc.utils.DataUtils;
import me.kamelajda.valorantrpc.utils.GameType;
import me.kamelajda.valorantrpc.utils.PresenceState;

import java.text.ParseException;

public class QueuePresence extends PresenceState {

    public QueuePresence(DiscordService discordService, LanguageService languageService) {
        super(discordService, languageService);
    }

    @Override
    public boolean tryApply(SocketData socketData, Activity activity) {
        if (!socketData.getStateDetails().getSessionLoopState().equals("MENUS") || !socketData.getStateDetails().getPartyState().equals("MATCHMAKING")) {
            return false;
        }

        GameType type = GameType.fromKey(socketData.getStateDetails().getQueueId());

        activity.setDetails(getTranslate("presences.queue.in_queue"));
        activity.setState(getTranslate(type.getTranslateKey()));

        try {
            activity.timestamps().setStart(DataUtils.formatDate(socketData.getStateDetails().getQueueEntryTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        getDiscordService().updateRP(activity);
        return true;
    }

}
