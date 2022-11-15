package me.kamelajda.valorantrpc.presences.ingame;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.jcm.discordgamesdk.activity.Activity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.components.ConfigComponent;
import me.kamelajda.valorantrpc.config.ConfigBoolean;
import me.kamelajda.valorantrpc.dto.ClientSession;
import me.kamelajda.valorantrpc.dto.SocketData;
import me.kamelajda.valorantrpc.services.DiscordService;
import me.kamelajda.valorantrpc.services.LanguageService;
import me.kamelajda.valorantrpc.services.LockFileService;
import me.kamelajda.valorantrpc.utils.Agent;
import me.kamelajda.valorantrpc.utils.Maps;
import me.kamelajda.valorantrpc.utils.PresenceState;
import me.kamelajda.valorantrpc.utils.Rank;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class InGamePresence extends PresenceState {

    private final LockFileService lockFileService;
    private final ConfigComponent configComponent;

    @Getter
    private final Map<String, Instant> matchStartMap = new HashMap<>();

    public InGamePresence(DiscordService discordService, LockFileService lockFileService, LanguageService languageService, ConfigComponent configComponent) {
        super(discordService, languageService);
        this.lockFileService = lockFileService;
        this.configComponent = configComponent;
    }

    @Override
    public boolean tryApply(SocketData socketData, Activity activity) {
        if (!socketData.getStateDetails().getSessionLoopState().equals("INGAME")) return false;

        long teamScore = socketData.getStateDetails().getPartyOwnerMatchScoreAllyTeam();
        long enemyScore = socketData.getStateDetails().getPartyOwnerMatchScoreEnemyTeam();

        String playerAgentId = null;
        String matchID = null;
        try {
            ClientSession session = lockFileService.getSession();
            JsonObject object = lockFileService.retrieveCoreGame();
            if (object != null) {
                matchID = object.get("MatchID").getAsString();

                if (!matchStartMap.containsKey(matchID)) matchStartMap.put(matchID, Instant.now());

                JsonObject match = lockFileService.retrieveMatch(matchID);
                for (JsonElement players : match.getAsJsonArray("Players")) {
                    JsonObject objectPlayer = players.getAsJsonObject();
                    if (objectPlayer.get("Subject").getAsString().equals(session.getPuuid())) {
                        playerAgentId = objectPlayer.get("CharacterID").getAsString();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (configComponent.getEntityById("showagent", ConfigBoolean.class).getValue() && !playerAgentId.isEmpty() && !playerAgentId.isBlank()) {
            Agent agent = lockFileService.getContentData().getAgents().get(UUID.fromString(playerAgentId));
            applyAgentData(agent, activity);
        }

        String team;

        switch (socketData.getStateDetails().getPartyOwnerMatchCurrentTeam()) {
            case "Blue":
            case "TeamOne":
                team = "presences.in_game.team.defender";
                break;
            case "Red":
            case "TeamTwo":
                team = "presences.in_game.team.attacker";
                break;
            case "":
                team = "presences.in_game.loading_map";
                break;
            default:
                team = getTranslate("presences.in_game.team.other", socketData.getStateDetails().getPartyOwnerMatchCurrentTeam());
        }

        Maps map = Maps.fromData(socketData.getStateDetails().getMatchMap());

        Rank rank = Rank.getFromTier(socketData.getStateDetails().getCompetitiveTier());

        activity.setDetails(String.format("%s | %s:%s", getTranslate(team), teamScore, enemyScore));

        if (rank != null) {
            StringBuilder stateBuilder = new StringBuilder();

            if (configComponent.getEntityById("showrank", ConfigBoolean.class).getValue()) {
                stateBuilder.append(getTranslate("presences.in_game.rank")).append(" ").append(getTranslate(rank.getTranslateKey()));
            }

            if (configComponent.getEntityById("showlevel", ConfigBoolean.class).getValue()) {
                boolean brackets = !stateBuilder.toString().isEmpty();
                stateBuilder.append(brackets ? " (" : "")
                    .append(socketData.getStateDetails().getAccountLevel()).append(" ")
                    .append(getTranslate("presences.in_game.level")).append(brackets ? ")" : "");
            }

            activity.setState(stateBuilder.toString());
        }

        if (configComponent.getEntityById("showmap", ConfigBoolean.class).getValue()) {
            activity.assets().setLargeImage(map.getKey());
            activity.assets().setLargeText(map.name());
        }

        if (matchID != null && getMatchStartMap().containsKey(matchID)) {
            activity.timestamps().setStart(getMatchStartMap().get(matchID));
        }

        getDiscordService().updateRP(activity);
        return true;
    }

}
