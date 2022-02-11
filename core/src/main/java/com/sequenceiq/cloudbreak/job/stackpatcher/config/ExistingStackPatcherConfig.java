package com.sequenceiq.cloudbreak.job.stackpatcher.config;

import java.util.Map;

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;

@Configuration
@ConfigurationProperties(prefix = "existing-stack-patcher")
public class ExistingStackPatcherConfig {

    @NotBlank
    private Integer intervalInHours;

    @NotBlank
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
