package org.phdezann.cn.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    public static String read(File file) {
        try {
            return org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void write(File file, String content) {
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void forceMkdir(File directory) {
        try {
            org.apache.commons.io.FileUtils.forceMkdir(directory);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
