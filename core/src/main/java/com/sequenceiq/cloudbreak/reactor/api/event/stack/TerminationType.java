package com.sequenceiq.cloudbreak.reactor.api.event.stack;

public enum TerminationType {

    REGULAR,
    FORCED,
    RECOVERY;

    public boolean isForced() {
        return this == TerminationType.FORCED;
    }

    public boolean isRecovery() {
        return this == TerminationType.RECOVERY;
    }

}
