package com.sequenceiq.cloudbreak.api.model;

public enum SssdTlsReqcertType {

    NEVER, ALLOW, TRY, DEMAND, HARD;

    public String getRepresentation() {
        return name().toLowerCase();
    }
}
