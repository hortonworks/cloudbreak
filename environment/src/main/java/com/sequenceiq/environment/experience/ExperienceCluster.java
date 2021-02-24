package com.sequenceiq.environment.experience;

import java.util.Objects;
import java.util.StringJoiner;

public class ExperienceCluster {

    private final String name;

    private final String experienceName;

    private final String status;

    private ExperienceCluster(Builder builder) {
        name = builder.name;
        experienceName = builder.experienceName;
        status = builder.status;
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

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExperienceCluster.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("experienceName='" + experienceName + "'")
                .add("status='" + status + "'")
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
        return Objects.equals(name, that.name) && Objects.equals(experienceName, that.experienceName) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, experienceName, status);
    }

    public static class Builder {

        private String name;

        private String status;

        private String experienceName;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder withExperienceName(String experienceName) {
            this.experienceName = experienceName;
            return this;
        }

        public ExperienceCluster build() {
            return new ExperienceCluster(this);
        }
    }
}
