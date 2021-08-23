package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUpgradeSettings {

    private final boolean skipValidations;

    private final boolean dataHubUpgradeEntitled;

    public InternalUpgradeSettings(boolean skipValidations, boolean dataHubUpgradeEntitled) {
        this.skipValidations = skipValidations;
        this.dataHubUpgradeEntitled = dataHubUpgradeEntitled;
    }

    public boolean isSkipValidations() {
        return skipValidations;
    }

    public boolean isDataHubUpgradeEntitled() {
        return dataHubUpgradeEntitled;
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
        return skipValidations == that.skipValidations && dataHubUpgradeEntitled == that.dataHubUpgradeEntitled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipValidations, dataHubUpgradeEntitled);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InternalUpgradeSettings.class.getSimpleName() + "[", "]")
                .add("skipValidations=" + skipValidations)
                .add("datahubUpgradeEntitled=" + dataHubUpgradeEntitled)
                .toString();
    }
}
