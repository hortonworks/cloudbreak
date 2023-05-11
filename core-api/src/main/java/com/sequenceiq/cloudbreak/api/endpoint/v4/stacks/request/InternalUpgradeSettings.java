package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUpgradeSettings {

    private final boolean skipValidations;

    private final boolean dataHubRuntimeUpgradeEntitled;

    private final boolean upgradePreparation;

    private final boolean rollingUpgradeEnabled;

    public InternalUpgradeSettings(boolean skipValidations, boolean dataHubRuntimeUpgradeEntitled) {
        this.skipValidations = skipValidations;
        this.dataHubRuntimeUpgradeEntitled = dataHubRuntimeUpgradeEntitled;
        this.rollingUpgradeEnabled = false;
        this.upgradePreparation = false;
    }

    public InternalUpgradeSettings(boolean skipValidations, boolean dataHubRuntimeUpgradeEntitled, boolean rollingUpgradeEnabled) {
        this.skipValidations = skipValidations;
        this.dataHubRuntimeUpgradeEntitled = dataHubRuntimeUpgradeEntitled;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.upgradePreparation = false;
    }

    @JsonCreator
    public InternalUpgradeSettings(@JsonProperty("skipValidations") boolean skipValidations,
            @JsonProperty("dataHubRuntimeUpgradeEntitled") boolean dataHubRuntimeUpgradeEntitled,
            @JsonProperty("upgradePreparation") boolean upgradePreparation,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled) {
        this.skipValidations = skipValidations;
        this.dataHubRuntimeUpgradeEntitled = dataHubRuntimeUpgradeEntitled;
        this.upgradePreparation = upgradePreparation;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
    }

    public boolean isSkipValidations() {
        return skipValidations;
    }

    public boolean isDataHubRuntimeUpgradeEntitled() {
        return dataHubRuntimeUpgradeEntitled;
    }

    public boolean isUpgradePreparation() {
        return upgradePreparation;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InternalUpgradeSettings that = (InternalUpgradeSettings) o;
        return skipValidations == that.skipValidations
                && dataHubRuntimeUpgradeEntitled == that.dataHubRuntimeUpgradeEntitled
                && upgradePreparation == that.upgradePreparation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipValidations, dataHubRuntimeUpgradeEntitled, upgradePreparation);
    }

    @Override
    public String toString() {
        return "InternalUpgradeSettings{" +
                "skipValidations=" + skipValidations +
                ", dataHubRuntimeUpgradeEntitled=" + dataHubRuntimeUpgradeEntitled +
                ", upgradePreparation=" + upgradePreparation +
                '}';
    }
}
