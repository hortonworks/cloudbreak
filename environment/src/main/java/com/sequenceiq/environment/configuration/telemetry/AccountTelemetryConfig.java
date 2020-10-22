package com.sequenceiq.environment.configuration.telemetry;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

@Component
@ConfigurationProperties(prefix = "environment.telemetry")
public class AccountTelemetryConfig {

    private List<AnonymizationRule> rules = new ArrayList<>();

    public List<AnonymizationRule> getRules() {
        return rules;
    }

    public void setRules(List<AnonymizationRule> rules) {
        this.rules = rules;
    }
}
