package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUpgradeSettings {

    private final boolean skipValidations;

    private final boolean dataHubRuntimeUpgradeEntitled;

    private final boolean dataHubOsUpgradeEntitled;

    public InternalUpgradeSettings(boolean skipValidations, boolean dataHubRuntimeUpgradeEntitled, boolean dataHubOsUpgradeEntitled) {
        this.skipValidations = skipValidations;
        this.dataHubRuntimeUpgradeEntitled = dataHubRuntimeUpgradeEntitled;
        this.dataHubOsUpgradeEntitled = dataHubOsUpgradeEntitled;
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
                && dataHubOsUpgradeEntitled == that.dataHubOsUpgradeEntitled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipValidations, dataHubRuntimeUpgradeEntitled, dataHubOsUpgradeEntitled);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InternalUpgradeSettings.class.getSimpleName() + "[", "]")
                .add("skipValidations=" + skipValidations)
                .add("dataHubUpgradeEntitled=" + dataHubRuntimeUpgradeEntitled)
                .add("dataHubOsUpgradeEntitled=" + dataHubOsUpgradeEntitled)
                .toString();
    }
}
