package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUpgradeSettings {

    private final boolean skipValidations;

    private final boolean upgradePreparation;

    private final boolean rollingUpgradeEnabled;

    private final boolean upgradeReinitiation;

    @JsonCreator
    public InternalUpgradeSettings(@JsonProperty("skipValidations") boolean skipValidations,
            @JsonProperty("upgradePreparation") boolean upgradePreparation,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("upgradeReinitiation") boolean upgradeReinitiation) {
        this.skipValidations = skipValidations;
        this.upgradePreparation = upgradePreparation;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.upgradeReinitiation = upgradeReinitiation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSkipValidations() {
        return skipValidations;
    }

    public boolean isUpgradePreparation() {
        return upgradePreparation;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    public boolean isUpgradeReinitiation() {
        return upgradeReinitiation;
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
                && upgradePreparation == that.upgradePreparation
                && rollingUpgradeEnabled == that.rollingUpgradeEnabled
                && upgradeReinitiation == that.upgradeReinitiation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipValidations, upgradePreparation, rollingUpgradeEnabled, upgradeReinitiation);
    }

    @Override
    public String toString() {
        return "InternalUpgradeSettings{" +
                "skipValidations=" + skipValidations +
                ", upgradePreparation=" + upgradePreparation +
                ", rollingUpgradeEnabled=" + rollingUpgradeEnabled +
                ", upgradeReinitiation=" + upgradeReinitiation +
                '}';
    }

    public static final class Builder {

        private boolean skipValidations;

        private boolean upgradePreparation;

        private boolean rollingUpgradeEnabled;

        private boolean upgradeReinitiation;

        private Builder() {
        }

        public Builder withSkipValidations(boolean skipValidations) {
            this.skipValidations = skipValidations;
            return this;
        }

        public Builder withUpgradePreparation(boolean upgradePreparation) {
            this.upgradePreparation = upgradePreparation;
            return this;
        }

        public Builder withRollingUpgradeEnabled(boolean rollingUpgradeEnabled) {
            this.rollingUpgradeEnabled = rollingUpgradeEnabled;
            return this;
        }

        public Builder withUpgradeReinitiation(boolean upgradeReinitiation) {
            this.upgradeReinitiation = upgradeReinitiation;
            return this;
        }

        public InternalUpgradeSettings build() {
            return new InternalUpgradeSettings(skipValidations, upgradePreparation, rollingUpgradeEnabled, upgradeReinitiation);
        }
    }
}
