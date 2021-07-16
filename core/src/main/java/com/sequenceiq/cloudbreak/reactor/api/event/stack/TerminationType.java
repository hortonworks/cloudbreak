package com.sequenceiq.cloudbreak.reactor.api.event.stack;

public enum TerminationType {

    REGULAR,
    FORCEDTERMINATION,
    RECOVERY;

    public boolean isForced() {
        return this == TerminationType.FORCEDTERMINATION;
    }

    public boolean isRecovery() {
        return this == TerminationType.RECOVERY;
    }

}
