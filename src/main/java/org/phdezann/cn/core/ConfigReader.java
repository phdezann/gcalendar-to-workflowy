package org.phdezann.cn.core;

import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ConfigReader {

    private final AppArgs appArgs;
    private final JsonSerializer jsonSerializer;

    public Config read() {
        var entries = appArgs.getConfigFiles() //
                .stream() //
                .map(configFile -> {
                    log.info("Loading configuration {}", configFile);
                    var json = getReadFileToString(configFile);
                    return jsonSerializer.readValue(json, Config.class);
                }) //
                .flatMap(config -> config.getEntries().entrySet().stream()) //
                .collect(toMap(Entry::getKey, Entry::getValue));
        return new Config(entries);

    }

    private static String getReadFileToString(File configFile) {
        try {
            return FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
