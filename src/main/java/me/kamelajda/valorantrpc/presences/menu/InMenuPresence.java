package me.kamelajda.valorantrpc.presences.menu;

import de.jcm.discordgamesdk.activity.Activity;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.components.ConfigComponent;
import me.kamelajda.valorantrpc.config.ConfigBoolean;
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

    private final ConfigComponent configComponent;
    private Instant firstSignal;

    public InMenuPresence(DiscordService discordService, LanguageService languageService, ConfigComponent configComponent) {
        super(discordService, languageService);
        this.configComponent = configComponent;
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
        if (configComponent.getEntityById("showrank", ConfigBoolean.class).getValue() && rank != null) {
            activity.assets().setSmallImage(rank.getKey());
            activity.assets().setSmallText(getTranslate(rank.getTranslateKey()));
        }

        if (gameType != null) activity.setState(getTranslate(gameType.getTranslateKey()));

        activity.timestamps().setStart(firstSignal);

        getDiscordService().updateRP(activity);
        return true;
    }

}
