package org.phdezann.cn.core;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.phdezann.cn.core.GoogleCalendar.WatchResponse;
import org.phdezann.cn.support.FileUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
public class ChannelCache {

    private final AppArgs appArgs;
    private final JsonSerializer jsonSerializer;
    private final CacheContent cacheContent;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class CacheValue {
        private String calendarId;
        private String resourceId;
        private String channelId;
        private ZonedDateTime expiration;

        public CacheValue(String calendarId, WatchResponse watchResponse) {
            this.calendarId = calendarId;
            this.resourceId = watchResponse.getResourceId();
            this.channelId = watchResponse.getChannelId();
            this.expiration = watchResponse.getExpiration();
        }
    }

    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class CacheContent {
        private Map<String, CacheValue> entries = new HashMap<>();
    }

    public ChannelCache(AppArgs appArgs, JsonSerializer jsonSerializer) {
        this.appArgs = appArgs;
        this.jsonSerializer = jsonSerializer;
        this.cacheContent = readOnDisk();
    }

    public List<CacheValue> getAllValues() {
        return new ArrayList<>(cacheContent //
                .entries //
                .values());
    }

    public void set(CacheValue cacheValue) {
        var key = DigestUtils.md5Hex(cacheValue.getCalendarId());
        cacheContent.getEntries().put(key, cacheValue);
        persist();
    }

    public void clear() {
        cacheContent.getEntries().clear();
        persist();
    }

    private CacheContent readOnDisk() {
        var cacheFile = getCacheFile();
        if (cacheFile.exists()) {
            var json = FileUtils.read(cacheFile);
            return jsonSerializer.readValue(json, CacheContent.class);
        } else {
            return new CacheContent();
        }
    }

    private void persist() {
        var json = jsonSerializer.writeValue(cacheContent);
        FileUtils.write(getCacheFile(), json);
    }

    private File getCacheFile() {
        var cacheDir = appArgs.getCacheDirectory();
        FileUtils.forceMkdir(cacheDir);
        return new File(cacheDir, "channel.json");
    }
}
