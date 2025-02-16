package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class DeploymentPreferencesV4Response {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<SupportedExternalDatabaseServiceEntryV4Response> supportedExternalDatabases = new HashSet<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<FeatureSwitchV4> featureSwitchV4s = new HashSet<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean platformSelectionDisabled;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Boolean> platformEnablement = new HashMap<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Boolean> govPlatformEnablement = new HashMap<>();

    private String controlPlaneRegion;

    public Set<SupportedExternalDatabaseServiceEntryV4Response> getSupportedExternalDatabases() {
        return supportedExternalDatabases;
    }

    public void setSupportedExternalDatabases(Set<SupportedExternalDatabaseServiceEntryV4Response> supportedExternalDatabases) {
        this.supportedExternalDatabases = supportedExternalDatabases;
    }

    public Set<FeatureSwitchV4> getFeatureSwitchV4s() {
        return featureSwitchV4s;
    }

    public void setFeatureSwitchV4s(Set<FeatureSwitchV4> featureSwitchV4s) {
        this.featureSwitchV4s = featureSwitchV4s;
    }

    public boolean isPlatformSelectionDisabled() {
        return platformSelectionDisabled;
    }

    public void setPlatformSelectionDisabled(boolean platformSelectionDisabled) {
        this.platformSelectionDisabled = platformSelectionDisabled;
    }

    public Map<String, Boolean> getPlatformEnablement() {
        return platformEnablement;
    }

    public void setPlatformEnablement(Map<String, Boolean> platformEnablement) {
        this.platformEnablement = platformEnablement;
    }

    public Map<String, Boolean> getGovPlatformEnablement() {
        return govPlatformEnablement;
    }

    public void setGovPlatformEnablement(Map<String, Boolean> govPlatformEnablement) {
        this.govPlatformEnablement = govPlatformEnablement;
    }

    public String getControlPlaneRegion() {
        return controlPlaneRegion;
    }

    public void setControlPlaneRegion(String controlPlaneRegion) {
        this.controlPlaneRegion = controlPlaneRegion;
    }

}
