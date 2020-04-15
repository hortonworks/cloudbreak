package com.sequenceiq.environment.api.v1.telemetry.model.request;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

public class TestAnonymizationRuleRequest implements Serializable {

    @NotNull
    @Size(max = 1000)
    private String input;

    @NotNull
    private AnonymizationRule rule;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public AnonymizationRule getRule() {
        return rule;
    }

    public void setRule(AnonymizationRule rule) {
        this.rule = rule;
    }
}
