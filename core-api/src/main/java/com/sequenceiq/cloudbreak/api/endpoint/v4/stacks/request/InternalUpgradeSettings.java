package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUpgradeSettings {

    private final boolean skipValidations;

    private final boolean dataHubRuntimeUpgradeEntitled;

    private final boolean dataHubOsUpgradeEntitled;

    private final boolean upgradePreparation;

    public InternalUpgradeSettings(boolean skipValidations, boolean dataHubRuntimeUpgradeEntitled, boolean dataHubOsUpgradeEntitled) {
        this.skipValidations = skipValidations;
        this.dataHubRuntimeUpgradeEntitled = dataHubRuntimeUpgradeEntitled;
        this.dataHubOsUpgradeEntitled = dataHubOsUpgradeEntitled;
        upgradePreparation = false;
    }

    @JsonCreator
    public InternalUpgradeSettings(@JsonProperty("skipValidations") boolean skipValidations,
            @JsonProperty("dataHubRuntimeUpgradeEntitled") boolean dataHubRuntimeUpgradeEntitled,
            @JsonProperty("dataHubOsUpgradeEntitled") boolean dataHubOsUpgradeEntitled,
            @JsonProperty("upgradePreparation") boolean upgradePreparation) {
        this.skipValidations = skipValidations;
        this.dataHubRuntimeUpgradeEntitled = dataHubRuntimeUpgradeEntitled;
        this.dataHubOsUpgradeEntitled = dataHubOsUpgradeEntitled;
        this.upgradePreparation = upgradePreparation;
    }

    public boolean isSkipValidations() {
        return skipValidations;
    }

    public boolean isDataHubRuntimeUpgradeEntitled() {
        return dataHubRuntimeUpgradeEntitled;
    }

    public boolean isDataHubOsUpgradeEntitled() {
        return dataHubOsUpgradeEntitled;
    }

    public boolean isUpgradePreparation() {
        return upgradePreparation;
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
                && dataHubOsUpgradeEntitled == that.dataHubOsUpgradeEntitled
                && upgradePreparation == that.upgradePreparation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipValidations, dataHubRuntimeUpgradeEntitled, dataHubOsUpgradeEntitled, upgradePreparation);
    }

    @Override
    public String toString() {
        return "InternalUpgradeSettings{" +
                "skipValidations=" + skipValidations +
                ", dataHubRuntimeUpgradeEntitled=" + dataHubRuntimeUpgradeEntitled +
                ", dataHubOsUpgradeEntitled=" + dataHubOsUpgradeEntitled +
                ", upgradePreparation=" + upgradePreparation +
                '}';
    }
}
