package com.sequenceiq.cloudbreak;

import java.util.concurrent.Callable;

public class PollingService implements Callable<Result> {
    private static final int SEC_IN_MILLIS = 1000;
    private int count;

    @Override
    public Result call() throws Exception {
        count++;
        Thread.sleep(SEC_IN_MILLIS);
        return new Result("iteration :" + count++);
    }
}
