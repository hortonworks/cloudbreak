package com.sequenceiq.cloudbreak.polling;

import java.time.Instant;

public class AbsolutTimeBasedTimeoutChecker implements TimeoutChecker {

    private final Instant timeoutInstant;

    public AbsolutTimeBasedTimeoutChecker(long maximumWaitTimeInSeconds) {
        timeoutInstant = Instant.now().plusSeconds(maximumWaitTimeInSeconds);
    }

    @Override
    public boolean checkTimeout() {
        return Instant.now().isAfter(timeoutInstant);
    }
}
