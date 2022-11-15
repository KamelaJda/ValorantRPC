package me.kamelajda.valorantrpc.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum Language {

    UNKNOWN(null, null, null),
    POLISH("pl_PL", "Polish", new Locale("pl", "PL")),
    ENGLISH("en_US", "English (US)", Locale.US),
    ;

    private final String key;
    private final String displayName;
    private final Locale locale;

    public static Language fromLocale(@NotNull String key) {
        for (Language value : values()) {
            if (value != UNKNOWN && value.getKey().equals(key)) return value;
        }
        return null;
    }

    public static boolean exist(String name) {
        for (Language value : values()) {
            if (value != UNKNOWN && value.name().equals(name)) return true;
        }
        return false;
    }

}
