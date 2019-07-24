package com.sequenceiq.cloudbreak.polling;

import java.time.Instant;
import java.time.temporal.TemporalUnit;

public class AbsolutTimeBasedTimeoutChecker implements TimeoutChecker {

    private final Instant timeoutInstant;

    public AbsolutTimeBasedTimeoutChecker(long waitSec) {
        timeoutInstant = Instant.now().plusSeconds(waitSec);
    }

    public AbsolutTimeBasedTimeoutChecker(long wait, TemporalUnit unit) {
        timeoutInstant = Instant.now().plus(wait, unit);
    }

    @Override
    public boolean checkTimeout() {
        return Instant.now().isAfter(timeoutInstant);
    }
}
