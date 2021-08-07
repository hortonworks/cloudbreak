package com.sequenceiq.cloudbreak.reactor.api.event.stack;

public enum ProvisionType {

    REGULAR,
    RECOVERY;

    public boolean isRecovery() {
        return this == ProvisionType.RECOVERY;
    }
}
