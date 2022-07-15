package com.sequenceiq.cloudbreak.cloud.aws.util.poller;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;

@Component
public class Poller<V> {

    public void runPoller(long sleepTimeInSeconds, long durationInMinutes, AttemptMaker<V> attemptMaker) {
        Polling.waitPeriodly(sleepTimeInSeconds, TimeUnit.SECONDS)
                .stopIfException(true)
                .stopAfterDelay(durationInMinutes, TimeUnit.MINUTES)
                .run(attemptMaker);

    }
}
