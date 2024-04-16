package org.phdezann.cn.core;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.LinkParser.WorkflowyLink;

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

    public void onNotification(String channelId) {
        log.trace("Got notification for channelId#{}", channelId);
        channelCache.getAllValues() //
                .stream() //
                .filter(cacheValue -> cacheValue.getChannelId().equals(channelId)) //
                .findAny() //
                .ifPresent(cacheValue -> createEvent(cacheValue.getCalendarId()));
    }

    private void createEvent(String calendarId) {
        googleCalendar.getEvents(calendarId) //
                .stream() //
                .filter(event -> event.getStatus().equals("confirmed")) //
                .forEach(event -> {
                    log.info("Event#{} '{}' to be synced", event.getId(), event.getSummary());

                    var workflowyBullet = eventFormatter.format(event);

                    var description = event.getDescription();
                    var workflowyLink = linkParser.extractWorkflowyLink(description);
                    var bulletId = workflowyLink.map(WorkflowyLink::getBulletId);
                    var result = workflowyClient.updateBullet(workflowyBullet.getTitle(), //
                            workflowyBullet.getNote(), //
                            bulletId);

                    var updatedDescription = descriptionUpdater.update(description, result.getId(), workflowyLink);
                    if (!StringUtils.equals(updatedDescription, description)) {
                        googleCalendar.updateDescription(calendarId, event.getId(), updatedDescription);
                        log.info("Updated description for event#{}", event.getId());
                    }
                });
    }

}
