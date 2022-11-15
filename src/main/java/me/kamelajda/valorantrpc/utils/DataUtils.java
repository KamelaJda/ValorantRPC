package me.kamelajda.valorantrpc.utils;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
public abstract class DataUtils {

    private DataUtils() { }

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");

    public static Instant formatDate(String date) throws ParseException {
        return SDF.parse(date).toInstant()
            .plus(1, ChronoUnit.HOURS);
            // FIXME: Why valorant is delayed?
    }

}
