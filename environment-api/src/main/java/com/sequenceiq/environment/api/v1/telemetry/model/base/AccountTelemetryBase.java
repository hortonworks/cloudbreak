package com.sequenceiq.environment.api.v1.telemetry.model.base;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.model.SensitiveLoggingComponent;

public class AccountTelemetryBase implements Serializable {

    private List<AnonymizationRule> rules;

    private Set<String> enabledSensitiveStorageLogs;

    public List<AnonymizationRule> getRules() {
        return rules;
    }

    public void setRules(List<AnonymizationRule> rules) {
        this.rules = rules;
    }

    public Set<String> getEnabledSensitiveStorageLogs() {
        return enabledSensitiveStorageLogs;
    }

    public void setEnabledSensitiveStorageLogs(Set<String> enabledSensitiveStorageLogs) {
        this.enabledSensitiveStorageLogs = enabledSensitiveStorageLogs;
    }

    public void setEnabledSensitiveStorageLogsByEnum(Set<SensitiveLoggingComponent> enabledSensitiveStorageLogs) {
        this.enabledSensitiveStorageLogs =
                CollectionUtils.emptyIfNull(enabledSensitiveStorageLogs).stream().map(SensitiveLoggingComponent::name).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "AccountTelemetryBase{" +
                "rules=" + rules +
                ", enabledSensitiveStorageLogs=" + enabledSensitiveStorageLogs +
                '}';
    }
}
