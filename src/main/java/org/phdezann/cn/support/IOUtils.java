package org.phdezann.cn.support;

import java.net.HttpURLConnection;
import java.net.URI;

public class IOUtils {

    public static boolean isUrlReachable(String url) {
        HttpURLConnection connection = null;
        try {
            var u = new URI(url).toURL();
            connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            connection.connect();
            return connection.getResponseCode() < 500;
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
