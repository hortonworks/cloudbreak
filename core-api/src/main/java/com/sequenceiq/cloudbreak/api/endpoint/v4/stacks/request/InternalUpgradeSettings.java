package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUpgradeSettings {

    private boolean skipValidations;

    public InternalUpgradeSettings() {
    }

    public InternalUpgradeSettings(boolean skipValidations) {
        this.skipValidations = skipValidations;
    }

    public boolean isSkipValidations() {
        return skipValidations;
    }

    public void setSkipValidations(boolean skipValidations) {
        this.skipValidations = skipValidations;
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
        return skipValidations == that.skipValidations;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipValidations);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InternalUpgradeSettings.class.getSimpleName() + "[", "]")
                .add("skipValidations=" + skipValidations)
                .toString();
    }
}
