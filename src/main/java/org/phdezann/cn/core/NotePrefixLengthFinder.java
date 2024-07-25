package org.phdezann.cn.core;

import java.util.regex.Pattern;

public class NotePrefixLengthFinder {

    public static int findPrefixLength(String text) {
        var p = Pattern.compile("([^\\p{Print}]+).*");
        var m = p.matcher(text);
        if (!m.matches()) {
            return 0;
        }
        return m.group(1).isEmpty() ? 0 : 5;
    }

}
