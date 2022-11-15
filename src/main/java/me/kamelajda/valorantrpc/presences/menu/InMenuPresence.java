package me.kamelajda.valorantrpc.presences.menu;

import de.jcm.discordgamesdk.activity.Activity;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.dto.SocketData;
import me.kamelajda.valorantrpc.presences.ingame.InGamePresence;
import me.kamelajda.valorantrpc.services.DiscordService;
import me.kamelajda.valorantrpc.services.LanguageService;
import me.kamelajda.valorantrpc.utils.GameType;
import me.kamelajda.valorantrpc.utils.PresenceState;
import me.kamelajda.valorantrpc.utils.Rank;

import java.time.Instant;

@Slf4j
public class InMenuPresence extends PresenceState {

    private Instant firstSignal;

    public InMenuPresence(DiscordService discordService, LanguageService languageService) {
        super(discordService, languageService);
    }

    @Override
    public boolean tryApply(SocketData socketData, Activity activity) {
        if (!socketData.getStateDetails().getSessionLoopState().equals("MENUS")
            || socketData.getStateDetails().isIdle()) return false;

        for (PresenceState state : getDiscordService().getStates()) {
            if (state instanceof InGamePresence) {
                ((InGamePresence) state).getMatchStartMap().clear();
                break;
            }
        }

        if (firstSignal == null) firstSignal = Instant.now();

        GameType gameType = GameType.fromKey(socketData.getStateDetails().getQueueId());

        activity.setDetails(getTranslate("presences.in_menu.in_menu"));

        Rank rank = Rank.getFromTier(socketData.getStateDetails().getCompetitiveTier());
        if (rank != null) {
            activity.assets().setSmallImage(rank.getKey());
            activity.assets().setSmallText(getTranslate(rank.getTranslateKey()));
        }

        if (gameType != null) activity.setState(getTranslate(gameType.getTranslateKey()));

        activity.timestamps().setStart(firstSignal);

        getDiscordService().updateRP(activity);
        return true;
    }

}
