package org.phdezann.cn.core;

import static org.phdezann.cn.core.DateTimeConverter.toZonedDateTime;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.EventFormatter.WorkflowyBullet;
import org.phdezann.cn.core.LinkParser.WorkflowyLink;
import org.phdezann.cn.core.WorkflowyClient.UpdateResult;

import com.google.api.services.calendar.model.Event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class EventCreator {

    private final GoogleCalendar googleCalendar;
    private final ChannelCache channelCache;
    private final EventFormatter eventFormatter;
    private final WorkflowyClient workflowyClient;
    private final LinkParser linkParser;
    private final DescriptionUpdater descriptionUpdater;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public synchronized void onNotification(String channelId) {
        log.trace("Got notification for channelId#{}", channelId);
        channelCache.getAllValues() //
                .stream() //
                .filter(cacheValue -> cacheValue.getChannelId().equals(channelId)) //
                .findAny() //
                .ifPresent(cacheValue -> createEvents(cacheValue.getCalendarId()));
    }

    public synchronized void createEventsOnStartup() {
        log.info("Synchronizing events with no notification");
        channelCache.getAllValues() //
                .forEach(cacheValue -> createEvents(cacheValue.getCalendarId()));
    }

    private void createEvents(String calendarId) {
        googleCalendar.getEvents(calendarId) //
                .stream() //
                .filter(event -> event.getStatus().equals("confirmed")) //
                .forEach(event -> {
                    log.info("Event#{} '{}' to be synced", event.getId(), event.getSummary());

                    var workflowyBullet = eventFormatter.format(event);

                    var description = event.getDescription();
                    var workflowyLink = linkParser.extractWorkflowyLink(description);
                    var bulletIdInDesc = workflowyLink.map(WorkflowyLink::getBulletId);
                    var currentBulletId = createOrUpdateBullet(event, workflowyBullet, bulletIdInDesc).getId();
                    var result = workflowyClient.updateBullet(workflowyBullet.getTitle(), //
                            workflowyBullet.getNote(), //
                            currentBulletId);

                    var updatedDescription = descriptionUpdater.update(description, result.getId(), workflowyLink);
                    if (!StringUtils.equals(updatedDescription, description)) {
                        googleCalendar.updateDescription(calendarId, event.getId(), updatedDescription);
                        log.info("Updated description for event#{}", event.getId());
                    }
                });
    }

    private UpdateResult createOrUpdateBullet(Event event, WorkflowyBullet workflowyBullet,
            Optional<String> bulletIdInDesc) {
        var title = workflowyBullet.getTitle();
        var note = workflowyBullet.getNote();

        if (bulletIdInDesc.isEmpty() || isNewlyCreatedEvent(event)) {
            return workflowyClient.createBullet(title, note);
        } else {
            return workflowyClient.updateBullet(title, note, bulletIdInDesc.orElseThrow());
        }
    }

    private boolean isNewlyCreatedEvent(Event event) {
        var created = toZonedDateTime(event.getCreated());
        var updated = toZonedDateTime(event.getUpdated());
        var diffInSeconds = ChronoUnit.SECONDS.between(created, updated);
        return diffInSeconds == 0;
    }

    public void setupEventsEveryDay() {
        var now = ZonedDateTime.now();

        var todayAt0630 = now //
                .withHour(6) //
                .withMinute(30) //
                .withSecond(0) //
                .withNano(0);
        scheduleEveryDay(todayAt0630, this::createEventsOnStartup);
    }

    private void scheduleEveryDay(ZonedDateTime tick, Runnable command) {
        var now = ZonedDateTime.now();
        if (tick.isBefore(now)) {
            tick = tick.plusDays(1);
        }
        var delay = Duration.between(now, tick).getSeconds();

        var oneDayInSeconds = 60 * 60 * 24;
        scheduler //
                .scheduleWithFixedDelay(command, delay, oneDayInSeconds, TimeUnit.SECONDS);
    }

}
