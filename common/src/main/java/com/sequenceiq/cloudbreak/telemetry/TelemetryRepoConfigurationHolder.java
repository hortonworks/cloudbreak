package com.sequenceiq.cloudbreak.telemetry;

import static com.sequenceiq.common.model.OsType.RHEL8;

import jakarta.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;

@Component
@ConfigurationProperties("telemetry.repos")
public class TelemetryRepoConfigurationHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRepoConfigurationHolder.class);

    @NotBlank
    private TelemetryRepoConfiguration rhel7;

    @NotBlank
    private TelemetryRepoConfiguration rhel8;

    public TelemetryRepoConfiguration selectCorrectRepoConfig(TelemetryContext context) {
        String osType = context.getOsType();
        if (RHEL8.getOs().equals(osType)) {
            return rhel8;
        }
        if ("redhat7".equals(osType)) {
            return rhel7;
        }
        LOGGER.warn("Unrecognized OsType {} selected for TelemetryContext using default fallback rhel7 repository", osType);
        return rhel7;
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
}
