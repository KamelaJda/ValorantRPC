package me.kamelajda.valorantrpc.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.UUID;

@Data
@AllArgsConstructor
@ToString
public class SocketData {

    private final String actor;
    private final String basic;
    private final String details;

    @SerializedName("game_name")
    private final String gameName;

    @SerializedName("game_tag")
    private final String gameTag;

    private final String location;

    private final String msg;

    private final String name;

    private final String patchline;

    private final String pid;

    private final String platform;

    @SerializedName("private")
    private final StateDetails stateDetails;

    @Data
    @AllArgsConstructor
    @ToString
    public static class StateDetails {

        @SerializedName("isValid")
        private boolean valid;

        private final String sessionLoopState;

        private final String partyOwnerSessionLoopState;

        private final String customGameName;

        private final String customGameTeam;

        private final String partyOwnerMatchMap;

        private final String partyOwnerMatchCurrentTeam;

        private final long partyOwnerMatchScoreAllyTeam;

        private final long partyOwnerMatchScoreEnemyTeam;

        private final String partyOwnerProvisioningFlow;

        private final String provisioningFlow;

        private final String matchMap;

        private final String partyId;

        @SerializedName("isPartyOwner")
        private boolean partyOwner;

        private final String partyState;

        private final String partyAccessibility;

        private final int maxPartySize;

        private final String queueId;

        private final boolean partyLFM;

        private final String partyClientVersion;

        private final int partySize;

        private final String tournamentId;

        private final String rosterId;

        private final long partyVersion;

        private final String queueEntryTime;

        private final UUID playerCardId;

        private final String playerTitleId;

        private final String preferredLevelBorderId;

        private final int accountLevel;

        private final int competitiveTier;

        private final int leaderboardPosition;

        private final boolean idle;

    }

}
