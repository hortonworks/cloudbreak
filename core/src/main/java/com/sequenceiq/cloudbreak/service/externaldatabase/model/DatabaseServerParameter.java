package com.sequenceiq.cloudbreak.service.externaldatabase.model;

import java.util.Map;
import java.util.Objects;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;

public class DatabaseServerParameter {

    private final DatabaseAvailabilityType availabilityType;

    private final String engineVersion;

    private final Map<String, Object> attributes;

    private DatabaseServerParameter(Builder builder) {
        this.availabilityType = builder.availabilityType;
        this.engineVersion = builder.engineVersion;
        this.attributes = builder.attributes != null ? builder.attributes : Map.of();
    }

    public DatabaseAvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DatabaseServerParameter that = (DatabaseServerParameter) o;

        if (availabilityType != that.availabilityType) {
            return false;
        }
        if (!Objects.equals(engineVersion, that.engineVersion)) {
            return false;
        }
        return attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        int result = availabilityType != null ? availabilityType.hashCode() : 0;
        result = 31 * result + (engineVersion != null ? engineVersion.hashCode() : 0);
        result = 31 * result + attributes.hashCode();
        return result;
    }

    public static class Builder {

        private DatabaseAvailabilityType availabilityType;

        private String engineVersion;

        private Map<String, Object> attributes;

        public Builder withAvailabilityType(DatabaseAvailabilityType availabilityType) {
            this.availabilityType = availabilityType;
            return this;
        }

        public Builder withEngineVersion(String engineVersion) {
            this.engineVersion = engineVersion;
            return this;
        }

        public Builder withAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public DatabaseServerParameter build() {
            return new DatabaseServerParameter(this);
        }
    }
}
