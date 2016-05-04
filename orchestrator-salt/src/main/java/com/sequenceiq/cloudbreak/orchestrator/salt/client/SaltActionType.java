package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SaltActionType {

    RUN("run"),
    STOP("stop");

    private String action;

    SaltActionType(String action) {
        this.action = action;
    }

    @JsonValue
    public String getAction() {
        return action;
    }


}
