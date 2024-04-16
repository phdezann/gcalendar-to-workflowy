package org.phdezann.cn.core;

import java.util.concurrent.CountDownLatch;

public class TerminationLock {

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public void signalAbnormalTermination() {
        countDownLatch.countDown();
    }

    public void waitForAbnormalTermination() {
        try {
            countDownLatch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

}
