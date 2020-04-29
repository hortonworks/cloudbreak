package com.sequenceiq.environment.api.v1.telemetry.model.response;

import java.io.Serializable;

public class TestAnonymizationRuleResponse implements Serializable {

    private String output;

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
