package me.kamelajda.valorantrpc.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Getter
public enum Rank {

    RANK_0(0),

    RANK_1(3),
    RANK_2(4),
    RANK_3(5),

    RANK_4(6),
    RANK_5(7),
    RANK_6(8),

    RANK_7(9),
    RANK_8(10),
    RANK_9(11),

    RANK_10(12),
    RANK_11(13),
    RANK_12(14),

    RANK_13(15),
    RANK_14(16),
    RANK_15(17),

    RANK_16(18),
    RANK_17(19),
    RANK_18(20),

    RANK_19(21),
    RANK_20(20),
    RANK_21(21),

    RANK_22(22),
    RANK_23(23),
    RANK_24(24),

    RANK_25(25),
    ;

    private final int index;

    public String getKey() {
        return "rank_" + this.index;
    }

    public String getTranslateKey() {
        return "utils.rank." + this.index;
    }

    public static Rank getFromTier(int tier) {
        for (Rank value : values()) {
            if (value.getIndex() == tier) return value;
        }
        log.error("Not found rank for tier {}", tier);
        return null;
    }

}
