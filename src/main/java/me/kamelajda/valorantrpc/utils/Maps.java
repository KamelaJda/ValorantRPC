package me.kamelajda.valorantrpc.utils;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@ToString
public enum Maps {

    UNKNOWN("null"),
    ASCENT("ascent"),
    BIND("duality"),
    BREEZE("foxtrot"),
    FRACTURE("canyon"),
    HAVEN("triad"),
    ICEBOX("port"),
    PEARL("pitt"),
    RANGE("range"),
    SPLIT("bonsai"),
    ;

    private final String apiName;

    public String getKey() {
        return "map_" + apiName;
    }
    public static Maps fromData(String matchMap) {
        String[] split = matchMap.split("/");
        for (Maps maps : values()) {
            if (maps.apiName.equalsIgnoreCase(split[split.length - 1])) {
                return maps;
            }
        }
        log.error("Unknown map for {}", matchMap);
        return UNKNOWN;
    }

}
