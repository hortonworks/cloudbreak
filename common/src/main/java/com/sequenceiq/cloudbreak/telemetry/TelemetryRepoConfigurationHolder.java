package com.sequenceiq.cloudbreak.telemetry;

import jakarta.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@Component
@ConfigurationProperties("telemetry.repos")
public class TelemetryRepoConfigurationHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRepoConfigurationHolder.class);

    @NotBlank
    private TelemetryRepoConfiguration rhel7;

    @NotBlank
    private TelemetryRepoConfiguration rhel8;

    @NotBlank
    private TelemetryRepoConfiguration rhel8Arm;

    public TelemetryRepoConfiguration selectCorrectRepoConfig(TelemetryContext context) {
        TelemetryRepoConfiguration rhel8RepoConfigBasedOnArch = switch (Architecture.fromStringWithFallback(context.getArchitecture())) {
            case ARM64 -> rhel8Arm;
            case X86_64, UNKNOWN -> rhel8;
        };
        return switch (OsType.getByOsTypeStringWithCentos7Fallback(context.getOsType())) {
            case RHEL8 -> rhel8RepoConfigBasedOnArch;
            case CENTOS7 -> rhel7;
        };
    }

    public TelemetryRepoConfiguration getRhel7() {
        return rhel7;
    }

    public void setRhel7(TelemetryRepoConfiguration rhel7) {
        this.rhel7 = rhel7;
    }

    public TelemetryRepoConfiguration getRhel8() {
        return rhel8;
    }

    public void setRhel8(TelemetryRepoConfiguration rhel8) {
        this.rhel8 = rhel8;
    }

    public TelemetryRepoConfiguration getRhel8Arm() {
        return rhel8Arm;
    }

    public void setRhel8Arm(TelemetryRepoConfiguration rhel8Arm) {
        this.rhel8Arm = rhel8Arm;
    }
}
