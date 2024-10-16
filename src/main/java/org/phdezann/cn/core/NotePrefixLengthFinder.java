package org.phdezann.cn.core;

import java.util.Optional;
import java.util.regex.Pattern;

public class NotePrefixLengthFinder {

    public static int findPrefixLength(Optional<String> textOpt) {
        if (textOpt.isEmpty()) {
            return 0;
        }
        var text = textOpt.orElseThrow();
        var startsWithEmoji = match(text, "\\p{IsEmoji}+.*");
        var startsWithDigit = match(text, "\\d+.*");
        return startsWithEmoji && !startsWithDigit ? 5 : 0;
    }

    private static boolean match(String text, String regex) {
        return Pattern.compile(regex) //
                .matcher(text) //
                .matches();
    }

}
