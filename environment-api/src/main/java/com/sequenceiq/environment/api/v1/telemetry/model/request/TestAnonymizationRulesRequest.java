package com.sequenceiq.environment.api.v1.telemetry.model.request;

import java.io.Serializable;

import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

public class TestAnonymizationRulesRequest implements Serializable {

    private String input;

    private AnonymizationRule newRule;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public AnonymizationRule getNewRule() {
        return newRule;
    }

    public void setNewRule(AnonymizationRule newRule) {
        this.newRule = newRule;
    }
}
