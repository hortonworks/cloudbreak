package com.sequenceiq.cloudbreak.domain;

public enum StopRestrictionReason {

    NONE ("Instances can be stopped."),
    EPHEMERAL_VOLUMES ("Instances with ephemeral volumes cannot be stopped."),
    SPOT_INSTANCES ("Spot instances cannot be stopped.");

    private String reason;

    StopRestrictionReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
