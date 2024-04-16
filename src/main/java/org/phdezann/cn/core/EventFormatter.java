package org.phdezann.cn.core;

import static org.phdezann.cn.core.WorkflowyFormatter.colored;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import org.phdezann.cn.core.WorkflowyFormatter.COLOR;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class EventFormatter {

    @RequiredArgsConstructor
    @Getter
    public static class WorkflowyBullet {
        private final String title;
        private final String note;

    }

    public WorkflowyBullet format(Event event) {
        var title = buildTitle(event);
        var note = buildNote(event);
        return new WorkflowyBullet(title, note);
    }

    private String buildTitle(Event event) {
        var summary = event.getSummary();
        var start = event.getStart();
        var end = event.getEnd();

        var dayStart = getDay(start);
        var dayEnd = isSameDay(start, end) ? Optional.<String> empty() : Optional.of(getDay(end));
        var hostname = "#build-on-" + getHostname();
        var dayInfo = getDayInfo(dayStart, dayEnd);

        return String.format("%s | %s %s", colored(summary, COLOR.GRAY), colored(dayInfo, COLOR.SKY), hostname);
    }

    private String getDayInfo(String dayStart, Optional<String> dayEnd) {
        if (dayEnd.isEmpty()) {
            return dayStart;
        }
        return String.format("%s au %s", dayStart, dayEnd.get());
    }

    private String buildNote(Event event) {
        var start = event.getStart();
        var end = event.getEnd();
        var htmlLink = String.format("<a href=\"%s\">Lien</a>", event.getHtmlLink());
        if (isDateOnly(start) && isDateOnly(end)) {
            return String.format("     %s", htmlLink);
        } else {
            var time = toZonedDateTime(start).format(DateTimeFormatter.ofPattern("HH:mm"));
            return String.format("     %s | %s", colored(time, COLOR.SKY), htmlLink);
        }
    }

    private String getDay(EventDateTime eventDateTime) {
        var pattern = DateTimeFormatter.ofPattern("d LLLL yyyy", Locale.FRENCH);
        return toLocalDate(eventDateTime).format(pattern);
    }

    private boolean isDateOnly(EventDateTime eventDateTime) {
        return eventDateTime.getDate() != null && eventDateTime.getDateTime() == null;
    }

    private boolean isSameDay(EventDateTime eventStart, EventDateTime eventEnd) {
        if (isDateOnly(eventStart) && isDateOnly(eventEnd)) {
            return toLocalDate(eventStart).plusDays(1).equals(toLocalDate(eventEnd));
        } else {
            return toZonedDateTime(eventStart).toLocalDate().equals(toZonedDateTime(eventEnd).toLocalDate());
        }
    }

    private LocalDate toLocalDate(EventDateTime eventDateTime) {
        if (isDateOnly(eventDateTime)) {
            return LocalDate.parse(eventDateTime.getDate().toStringRfc3339());
        } else {
            return ZonedDateTime.parse(eventDateTime.getDateTime().toStringRfc3339()).toLocalDate();
        }
    }

    private ZonedDateTime toZonedDateTime(EventDateTime eventDateTime) {
        if (isDateOnly(eventDateTime)) {
            throw new RuntimeException("Must be a datetime");
        }
        return ZonedDateTime.parse(eventDateTime.getDateTime().toStringRfc3339());
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

}
