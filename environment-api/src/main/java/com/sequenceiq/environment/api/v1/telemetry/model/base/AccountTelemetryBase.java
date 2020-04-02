package com.sequenceiq.environment.api.v1.telemetry.model.base;

import java.io.Serializable;
import java.util.List;

import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

public class AccountTelemetryBase implements Serializable {

    private List<AnonymizationRule> rules;

    public List<AnonymizationRule> getRules() {
        return rules;
    }

    public void setRules(List<AnonymizationRule> rules) {
        this.rules = rules;
    }
}
