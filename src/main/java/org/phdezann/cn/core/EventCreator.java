package org.phdezann.cn.core;

import static org.phdezann.cn.core.model.EventStatusEnum.CANCELLED;
import static org.phdezann.cn.core.model.EventStatusEnum.OTHER;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.BulletCache.CacheValue;
import org.phdezann.cn.core.Config.ConfigKey;
import org.phdezann.cn.core.EventFormatter.WorkflowyBullet;
import org.phdezann.cn.core.converter.EventConverter;
import org.phdezann.cn.core.converter.EventStatusEnumConverter;
import org.phdezann.cn.core.model.Event;
import org.phdezann.cn.support.FileUtils;
import org.phdezann.cn.wf.core.WorkflowyClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class EventCreator {

    private final AppArgs appArgs;
    private final Config config;
    private final GoogleCalendar googleCalendar;
    private final EventStatusEnumConverter eventStatusEnumConverter;
    private final EventConverter eventConverter;
    private final EventFormatter eventFormatter;
    private final WorkflowyClient workflowyClient;
    private final BulletCache bulletCache;
    private final JsonSerializer jsonSerializer;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void startPolling() {
        log.info("Starting calendar polling every 5 minutes");
        pollSafely();
        scheduler.scheduleWithFixedDelay(this::pollSafely, 5, 5, TimeUnit.MINUTES);
    }

    private void pollSafely() {
        try {
            syncAllCalendars();
        } catch (Exception ex) {
            log.error("Got error while polling calendars", ex);
        }
    }

    public synchronized void syncAllCalendars() {
        log.info("Synchronizing events by polling");
        googleCalendar //
                .getCalendars(config.get(ConfigKey.CALENDAR_TITLES)) //
                .forEach(calendarId -> {
                    try {
                        createEvents(calendarId);
                    } catch (Exception ex) {
                        log.error("Got error while synchronizing calendar#{}", calendarId, ex);
                    }
                });
    }

    private void createEvents(String calendarId) {
        googleCalendar //
                .getEvents(calendarId) //
                .stream() //
                .map(event -> {
                    var status = eventStatusEnumConverter.convert(event.getStatus());
                    if (status == CANCELLED) {
                        return googleCalendar.getEvent(calendarId, event.getId());
                    }
                    return event;
                }) //
                .peek(this::dumpEvent) //
                .map(eventConverter::convert) //
                .forEach(this::createEvent);
    }

    private void createEvent(Event event) {
        if (event.getStatus() == OTHER) {
            log.warn("Ignoring event with status {}", event.getStatus());
            return;
        }

        var obsolete = isObsolete(event);
        if (obsolete) {
            log.info("Event#{} obsolete, no update", event.getId());
            return;
        }

        log.info("Event#{} '{}' '{}' to be synced", event.getId(), event.getSummary().orElse(""), event.getStatus());
        var workflowyBullet = formatWorkflowyBullet(event);
        var bulletId = createOrUpdateBullet(event, workflowyBullet);
        log.info("Bullet#{} in sync", bulletId);
    }

    private boolean isObsolete(Event event) {
        var diffInDays = ChronoUnit.DAYS.between(getEndDate(event), LocalDate.now());
        return diffInDays > 7;
    }

    private LocalDate getEndDate(Event event) {
        var end = event.getEnd();
        return end.isDateValue() ? end.getDate() : end.getTime().toLocalDate();
    }

    private void dumpEvent(com.google.api.services.calendar.model.Event event) {
        var eventDir = appArgs.getEventDir();
        if (eventDir == null) {
            return;
        }
        FileUtils.forceMkdir(eventDir);
        var json = jsonSerializer.writeValue(event);
        var output = new File(eventDir, UUID.randomUUID().toString());
        FileUtils.write(output, json);
        log.info("Dumped Event#{} to '{}'", event.getId(), output);
    }

    private WorkflowyBullet formatWorkflowyBullet(Event event) {
        return switch (event.getStatus()) {
            case CONFIRMED -> eventFormatter.formatConfirmed(event);
            case CANCELLED -> eventFormatter.formatCancelledEvent(event);
            default -> throw new IllegalArgumentException();
        };
    }

    private String createOrUpdateBullet(Event event, WorkflowyBullet workflowyBullet) {
        var cachedValue = bulletCache.get(event.getId());
        if (!hasChanged(workflowyBullet, cachedValue)) {
            return cachedValue.orElseThrow().bulletId();
        }
        var bulletId = workflowyClient //
                .createOrEditNode(event.getHtmlLink(), workflowyBullet.title(), workflowyBullet.note()) //
                .nodeShortId();
        updateCache(event.getId(), bulletId, workflowyBullet);
        return bulletId;
    }

    private boolean hasChanged(WorkflowyBullet workflowyBullet, Optional<CacheValue> cachedValue) {
        return cachedValue.stream() //
                .noneMatch(elt -> StringUtils.equals(elt.bulletTitle(), workflowyBullet.title()) //
                                  && StringUtils.equals(elt.bulletNote(), workflowyBullet.note()));
    }

    private void updateCache(String eventId, String bulletId, WorkflowyBullet workflowyBullet) {
        bulletCache.set(new CacheValue(eventId, bulletId, workflowyBullet.title(), workflowyBullet.note()));
    }

    public void shutdown() {
        scheduler.shutdown();
    }

}
