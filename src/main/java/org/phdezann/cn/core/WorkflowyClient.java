package org.phdezann.cn.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.phdezann.cn.core.Config.ConfigKey;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
public class WorkflowyClient {

    private final AppArgs appArgs;
    private final Config config;
    private final JsonSerializer jsonSerializer;

    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class UpdateResult {
        private String id;
        private String result;
    }

    public UpdateResult updateBullet(String name, String note, Optional<String> itemId) {
        var url = buildUrl(name, note, itemId.orElse(""));
        var json = getContent(url);
        return jsonSerializer.readValue(json, UpdateResult.class);
    }

    private String buildUrl(String name, String note, String itemId) {
        try {
            var port = config.get(ConfigKey.NODE_HTTP_SERVER_PORT);
            URIBuilder ub = new URIBuilder(String.format("http://localhost:%s/update", port));
            ub.addParameter("configFile", appArgs.getConfigFiles().iterator().next().getAbsolutePath());
            ub.addParameter("name", name);
            ub.addParameter("note", note);
            ub.addParameter("itemId", itemId);
            return ub.toString();
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getContent(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder() //
                    .uri(new URI(url)) //
                    .GET() //
                    .build();
            return HttpClient.newBuilder().build().send(request, BodyHandlers.ofString()).body();
        } catch (URISyntaxException | IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
