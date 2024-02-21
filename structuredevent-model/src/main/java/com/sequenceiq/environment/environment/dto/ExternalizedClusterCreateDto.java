package com.sequenceiq.environment.environment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ExternalizedClusterCreateDto.Builder.class)
public class ExternalizedClusterCreateDto {

    private static final String EXTERNALIZED_CLUSTER_NAME_FORMAT = "default-%s-compute-cluster";

    private String name;

    private ExternalizedClusterCreateDto(ExternalizedClusterCreateDto.Builder builder) {
        name = builder.name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ExternalizedClusterCreateDto{" +
                "name='" + name + '\'' +
                '}';
    }

    @JsonPOJOBuilder
    public static class Builder {

        private String name;

        private Builder() {
        }

        public ExternalizedClusterCreateDto.Builder withEnvName(String name) {
            this.name = String.format(EXTERNALIZED_CLUSTER_NAME_FORMAT, name);
            return this;
        }

        public ExternalizedClusterCreateDto build() {
            return new ExternalizedClusterCreateDto(this);
        }
    }
}
