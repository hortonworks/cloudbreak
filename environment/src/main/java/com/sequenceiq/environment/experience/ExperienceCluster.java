package com.sequenceiq.environment.experience;

import java.util.Objects;
import java.util.StringJoiner;

public class ExperienceCluster {

    private final String name;

    private final String experienceName;

    private final String status;

    private final String statusReason;

    private final String publicName;

    private ExperienceCluster(Builder builder) {
        name = builder.name;
        experienceName = builder.experienceName;
        status = builder.status;
        statusReason = builder.statusReason;
        publicName = builder.publicName;
    }

    public String getName() {
        return name;
    }

    public String getExperienceName() {
        return experienceName;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public String getPublicName() {
        return publicName;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExperienceCluster.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("experienceName='" + experienceName + "'")
                .add("status='" + status + "'")
                .add("statusReason='" + statusReason + "'")
                .add("publicName='" + publicName + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        ExperienceCluster that = (ExperienceCluster) o;
        return Objects.equals(name, that.name)
                && Objects.equals(experienceName, that.experienceName)
                && Objects.equals(status, that.status)
                && Objects.equals(publicName, that.publicName)
                && Objects.equals(statusReason, that.statusReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, experienceName, status, statusReason, publicName);
    }

    public static class Builder {

        private String name;

        private String status;

        private String experienceName;

        private String statusReason;

        private String publicName;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder withStatusReason(String statusReason) {
            this.statusReason = statusReason;
            return this;
        }

        public Builder withExperienceName(String experienceName) {
            this.experienceName = experienceName;
            return this;
        }

        public Builder withPublicName(String publicName) {
            this.publicName = publicName;
            return this;
        }

        public ExperienceCluster build() {
            return new ExperienceCluster(this);
        }
    }

}
