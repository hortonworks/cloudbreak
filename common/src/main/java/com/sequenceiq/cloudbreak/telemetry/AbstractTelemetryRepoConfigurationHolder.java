package com.sequenceiq.cloudbreak.telemetry;

import static com.sequenceiq.common.model.Architecture.ARM64;
import static com.sequenceiq.common.model.Architecture.X86_64;
import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

public abstract class AbstractTelemetryRepoConfigurationHolder {

    @NotBlank
    private String name;

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String gpgKey;

    @NotNull
    private Integer gpgCheck;

    @NotNull
    private Map<OsType, Map<Architecture, String>> platformValues;

    public TelemetryRepoConfiguration selectCorrectRepoConfig(TelemetryContext context) {
        TelemetryRepoConfiguration rhel8RepoConfigBasedOnArch = switch (Architecture.fromStringWithFallback(context.getArchitecture())) {
            case ARM64 -> getRepoConfigByPlatformValue(platformValues.get(RHEL8).get(ARM64));
            case X86_64, UNKNOWN -> getRepoConfigByPlatformValue(platformValues.get(RHEL8).get(X86_64));
        };
        return switch (OsType.getByOsTypeStringWithCentos7Fallback(context.getOsType())) {
            case RHEL8 -> rhel8RepoConfigBasedOnArch;
            case CENTOS7 -> getRepoConfigByPlatformValue(platformValues.get(CENTOS7).get(X86_64));
        };
    }

    private TelemetryRepoConfiguration getRepoConfigByPlatformValue(String platformValue) {
        return new TelemetryRepoConfiguration(name, String.format(baseUrl, platformValue), String.format(gpgKey, platformValue), gpgCheck);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getGpgKey() {
        return gpgKey;
    }

    public void setGpgKey(String gpgKey) {
        this.gpgKey = gpgKey;
    }

    public Integer getGpgCheck() {
        return gpgCheck;
    }

    public void setGpgCheck(Integer gpgCheck) {
        this.gpgCheck = gpgCheck;
    }

    public Map<OsType, Map<Architecture, String>> getPlatformValues() {
        return platformValues;
    }

    public void setPlatformValues(Map<OsType, Map<Architecture, String>> platformValues) {
        this.platformValues = platformValues;
    }
}
