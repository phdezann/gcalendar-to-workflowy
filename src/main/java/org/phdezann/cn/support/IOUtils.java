package org.phdezann.cn.support;

import java.net.URL;

public class IOUtils {

    public static boolean isUrlReachable(String url) {
        try {
            var connection = new URL(url).openConnection();
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
