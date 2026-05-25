package org.phdezann.cn.core;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GoogleCalendar {

    private final Config config;
    private final GoogleClient googleClient;
    private final Calendar calendar;
    private final SyncTokenCache syncTokenCache;

    public GoogleCalendar(Config config, GoogleClient googleClient, SyncTokenCache syncTokenCache) {
        this.config = config;
        this.googleClient = googleClient;
        this.calendar = getService();
        this.syncTokenCache = syncTokenCache;
    }

    public List<String> getCalendars(String titles) {
        var listOfTitle = Arrays //
                .stream(titles.split(",")) //
                .map(StringUtils::trim) //
                .toList();
        try {
            return calendar //
                    .calendarList() //
                    .list() //
                    .execute() //
                    .getItems() //
                    .stream() //
                    .filter(item -> listOfTitle.contains(item.getSummary())) //
                    .map(CalendarListEntry::getId) //
                    .toList();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Event> getEvents(String calendarId) {
        return getEvents(calendarId, true);
    }

    private List<Event> getEvents(String calendarId, boolean allowTokenReset) {
        try {
            var request = calendar.events().list(calendarId);
            var syncToken = syncTokenCache.get(calendarId);
            if (syncToken.isEmpty()) {
                var nowMinus1Min = new DateTime(ZonedDateTime //
                                                        .now() //
                                                        .minusMinutes(1) //
                                                        .withZoneSameInstant(ZoneId.of("UTC")) //
                                                        .toEpochSecond() * 1000);
                log.debug("Getting all events from {}", nowMinus1Min);
                request.setUpdatedMin(nowMinus1Min);
            } else {
                log.debug("Using previously received 'SyncToken' for calendar#{}", calendarId);
                request.setSyncToken(syncToken.orElseThrow());
            }

            var events = new ArrayList<Event>();
            String pageToken = null;
            String nextSyncToken = null;
            do {
                if (pageToken != null) {
                    request.setPageToken(pageToken);
                }
                var response = request.execute();
                var items = response.getItems();
                if (items != null) {
                    events.addAll(items);
                }
                pageToken = response.getNextPageToken();
                nextSyncToken = response.getNextSyncToken();
            } while (pageToken != null);

            if (nextSyncToken != null) {
                syncTokenCache.set(calendarId, nextSyncToken);
            }
            return events;
        } catch (GoogleJsonResponseException ex) {
            if (allowTokenReset && ex.getStatusCode() == HttpStatus.SC_GONE) {
                log.info("Sync token for calendar#{} expired, resetting it", calendarId);
                syncTokenCache.remove(calendarId);
                return getEvents(calendarId, false);
            }
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Event getEvent(String calendarId, String eventId) {
        try {
            return calendar.events().get(calendarId, eventId).execute();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Calendar getService() {
        var credentials = googleClient.getCredentials();
        HttpRequestInitializer requestInitializer = (com.google.api.client.http.HttpRequest request) -> {
            credentials.initialize(request);
            request.setConnectTimeout(10_000);
            request.setReadTimeout(20_000);
        };
        return new Calendar //
                .Builder(credentials.getTransport(), credentials.getJsonFactory(), requestInitializer) //
                .setApplicationName(GoogleClient.APPLICATION_NAME) //
                .build();
    }

}
