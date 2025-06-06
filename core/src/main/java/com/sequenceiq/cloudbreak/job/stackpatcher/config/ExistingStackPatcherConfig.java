package com.sequenceiq.cloudbreak.job.stackpatcher.config;

import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;

@Component
@ConfigurationProperties(prefix = "existing-stack-patcher")
public class ExistingStackPatcherConfig {

    @NotNull
    @Min(1)
    private Integer intervalInHours;

    @NotNull
    @Min(0)
    private Integer maxInitialStartDelayInHours;

    private Map<StackPatchType, StackPatchTypeConfig> patchConfigs;

    public int getIntervalInHours() {
        return intervalInHours;
    }

    public void setIntervalInHours(int intervalInHours) {
        this.intervalInHours = intervalInHours;
    }

    public int getMaxInitialStartDelayInHours() {
        return maxInitialStartDelayInHours;
    }

    public void setMaxInitialStartDelayInHours(int maxInitialStartDelayInHours) {
        this.maxInitialStartDelayInHours = maxInitialStartDelayInHours;
    }

    public Map<StackPatchType, StackPatchTypeConfig> getPatchConfigs() {
        return patchConfigs;
    }

    public void setPatchConfigs(Map<StackPatchType, StackPatchTypeConfig> patchConfigs) {
        this.patchConfigs = patchConfigs;
    }
}
