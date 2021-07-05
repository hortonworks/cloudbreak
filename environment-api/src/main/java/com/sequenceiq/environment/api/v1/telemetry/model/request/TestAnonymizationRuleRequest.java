package com.sequenceiq.environment.api.v1.telemetry.model.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

public class TestAnonymizationRuleRequest implements Serializable {

    @NotNull
    @Size(max = 1000)
    private String input;

    @NotNull
    private List<AnonymizationRule> rules = new ArrayList<>();

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setRules(List<AnonymizationRule> rules) {
        this.rules = rules;
    }

    public List<AnonymizationRule> getRules() {
        return rules;
    }

    @Override
    public String toString() {
        return "TestAnonymizationRuleRequest{" +
                "input='" + input + '\'' +
                ", rules=" + rules +
                '}';
    }
}
