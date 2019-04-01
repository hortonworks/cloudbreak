package com.sequenceiq.cloudbreak.domain;

public enum StopRestrictionReason {

    NONE("Instances can be stopped."),
    EPHEMERAL_VOLUMES("Instances with ephemeral volumes cannot be stopped.");
    private final String reason;

    StopRestrictionReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
