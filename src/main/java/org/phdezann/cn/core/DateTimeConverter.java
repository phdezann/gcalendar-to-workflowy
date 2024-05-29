package org.phdezann.cn.core;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.google.api.client.util.DateTime;

public class DateTimeConverter {

    private DateTimeConverter() {
    }

    public static LocalDate toLocalDate(DateTime dateTime) {
        return LocalDate.parse(dateTime.toStringRfc3339());
    }

    public static ZonedDateTime toZonedDateTime(DateTime dateTime) {
        return ZonedDateTime.parse(dateTime.toStringRfc3339());
    }
}
