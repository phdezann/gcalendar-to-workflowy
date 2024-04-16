package org.phdezann.cn.core;

import java.util.List;

import org.phdezann.cn.core.Config.ConfigKey;
import org.phdezann.cn.support.ProcessUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class NodeServerForker {

    private final Config config;
    private final TerminationLock terminationLock;
    private long pid;

    public void startNodeServer() {
        var process = forkProcess();
        this.pid = process.pid();
        new Thread(() -> waitForTermination(process)).start();
    }

    private Process forkProcess() {
        var port = config.get(ConfigKey.NODE_HTTP_SERVER_PORT);
        var args = List.of("node", "src/main/resources/js/workflowy-cli-js/main.js", //
                "--port", port);
        return ProcessUtils.create(args);
    }

    private void waitForTermination(Process process) {
        try {
            ProcessUtils.waitForTermination(process);
        } catch (Exception ex) {
            log.error("Node process has stopped", ex);
            terminationLock.signalAbnormalTermination();
        }
    }

    public void shutdown() {
        try {
            var args = List.of("kill", "-9", String.valueOf(pid));
            ProcessUtils.waitForTermination(ProcessUtils.create(args));
        } catch (Exception ex) {
            log.error("Error while kill node server", ex);
        }
    }
}
