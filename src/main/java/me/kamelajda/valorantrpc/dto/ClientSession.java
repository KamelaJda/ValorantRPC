package me.kamelajda.valorantrpc.dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class ClientSession {

    private boolean federated;

    @SerializedName("game_name")
    private String gameName;

    @SerializedName("game_Tag")
    private String gameTag;

    private boolean loaded;

    private String name;

    private String pid;

    private String puuid;

    private String region;

    private String resource;

    private String state;

}
