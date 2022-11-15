package me.kamelajda.valorantrpc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.kamelajda.valorantrpc.utils.Agent;
import me.kamelajda.valorantrpc.utils.GameType;

import java.util.Map;
import java.util.UUID;

@NoArgsConstructor
@Data
@ToString
public class ContentData {

    private Map<UUID, Agent> agents;

    private Map<String, GameType> gameTypes;

}
