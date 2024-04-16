package org.phdezann.cn.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessUtils {

    private ProcessUtils() {
    }

    public static Process create(List<String> args) {
        try {
            log.info("{}", args);
            return new ProcessBuilder(args).redirectErrorStream(true).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String waitForTermination(Process process) {
        try {
            InputStreamConsumer streamConsumer = new InputStreamConsumer(process.getInputStream());
            Thread streamConsumerThread = new Thread(streamConsumer);
            streamConsumerThread.start();
            int exitValue = process.waitFor();
            streamConsumerThread.join();
            String output = streamConsumer.getOutput();
            if (exitValue != 0) {
                throw new IllegalStateException(
                        String.format("exitValue was:'%s' (not zero), with output:'%s'", exitValue, output));
            }
            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @RequiredArgsConstructor
    @Getter
    public static class InputStreamConsumer implements Runnable {

        private final InputStream inputStream;

        private String output;

        @Override
        public void run() {
            try {
                this.output = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

}
