package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class Orchestrator extends StringType {

    private Orchestrator(String value) {
        super(value);
    }

    public static Orchestrator orchestrator(String value) {
        return new Orchestrator(value);
    }

}
