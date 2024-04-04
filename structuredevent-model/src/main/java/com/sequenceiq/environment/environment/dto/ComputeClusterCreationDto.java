package com.sequenceiq.environment.environment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ComputeClusterCreationDto.Builder.class)
public class ComputeClusterCreationDto {

    private boolean createComputeCluster;

    private ComputeClusterCreationDto(ComputeClusterCreationDto.Builder builder) {
        createComputeCluster = builder.createComputeCluster;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isCreateComputeCluster() {
        return createComputeCluster;
    }

    public void setCreateComputeCluster(boolean createComputeCluster) {
        this.createComputeCluster = createComputeCluster;
    }

    @Override
    public String toString() {
        return "ComputeClusterCreationDto{" +
                "createComputeCluster='" + createComputeCluster + '\'' +
                '}';
    }

    @JsonPOJOBuilder
    public static class Builder {

        private boolean createComputeCluster;

        private Builder() {
        }

        public ComputeClusterCreationDto.Builder withCreateComputeCluster(boolean createComputeCluster) {
            this.createComputeCluster = createComputeCluster;
            return this;
        }

        public ComputeClusterCreationDto build() {
            return new ComputeClusterCreationDto(this);
        }
    }
}