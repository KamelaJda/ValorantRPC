package me.kamelajda.valorantrpc.presences.ingame;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.jcm.discordgamesdk.activity.Activity;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.dto.ClientSession;
import me.kamelajda.valorantrpc.dto.SocketData;
import me.kamelajda.valorantrpc.services.DiscordService;
import me.kamelajda.valorantrpc.services.LanguageService;
import me.kamelajda.valorantrpc.services.LockFileService;
import me.kamelajda.valorantrpc.utils.Agent;
import me.kamelajda.valorantrpc.utils.Maps;
import me.kamelajda.valorantrpc.utils.PresenceState;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PreGamePresence extends PresenceState {

    private final LockFileService lockFileService;

    public PreGamePresence(DiscordService discordService, LockFileService lockFileService, LanguageService languageService) {
        super(discordService, languageService);
        this.lockFileService= lockFileService;
    }

    @Override
    public boolean tryApply(SocketData socketData, Activity activity) {
        if (!socketData.getStateDetails().getSessionLoopState().equals("PREGAME")) return false;

        try {
            ClientSession session = lockFileService.getSession();

            String matchID = lockFileService.retrievePregamePlayer().get("MatchID").getAsString();
            JsonObject pregameMatch = lockFileService.retrievePregameMatch(matchID);

            JsonObject playerData = null;
            for (JsonElement players : pregameMatch.getAsJsonObject("AllyTeam").getAsJsonArray("Players")) {
                JsonObject playerObject = players.getAsJsonObject();
                if (playerObject.get("Subject").getAsString().equals(session.getPuuid())) {
                    playerData = playerObject;
                    break;
                }
            }

            long phaseTimeRemainingNS = TimeUnit.MILLISECONDS.convert(pregameMatch.get("PhaseTimeRemainingNS").getAsLong(), TimeUnit.NANOSECONDS) + System.currentTimeMillis() + 1_000;

            String agentState = playerData.get("CharacterSelectionState").getAsString();
            String characterID = playerData.get("CharacterID").getAsString();

            if (!characterID.isBlank() && !characterID.isEmpty()) {
                Agent agent = lockFileService.getContentData().getAgents().get(UUID.fromString(characterID));

                String key = "presences.pregame.agent_status";
                if (agentState.equals("locked")) key += ".locked";

                activity.setState(getTranslate(key, getTranslate(agent.getTranslateKey())));
                applyAgentData(agent, activity);
            }

            activity.setDetails(getTranslate("presences.pregame.chooses"));
            activity.timestamps().setEnd(Instant.from(new Date(phaseTimeRemainingNS).toInstant()));

            Maps map = Maps.fromData(pregameMatch.get("MapID").getAsString());
            activity.assets().setLargeImage(map.getKey());
            activity.assets().setLargeText(map.name());

            getDiscordService().updateRP(activity);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

}
