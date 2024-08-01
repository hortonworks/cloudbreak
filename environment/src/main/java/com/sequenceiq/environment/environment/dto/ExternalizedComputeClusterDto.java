package com.sequenceiq.environment.environment.dto;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ExternalizedComputeClusterDto.Builder.class)
public class ExternalizedComputeClusterDto {

    private boolean create;

    private boolean privateCluster;

    private Set<String> kubeApiAuthorizedIpRanges;

    private String outboundType;

    private Set<String> workerNodeSubnetIds;

    private ExternalizedComputeClusterDto(ExternalizedComputeClusterDto.Builder builder) {
        create = builder.create;
        privateCluster = builder.privateCluster;
        kubeApiAuthorizedIpRanges = builder.kubeApiAuthorizedIpRanges;
        outboundType = builder.outboundType;
        workerNodeSubnetIds = builder.workerNodeSubnetIds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isPrivateCluster() {
        return privateCluster;
    }

    public void setPrivateCluster(boolean privateCluster) {
        this.privateCluster = privateCluster;
    }

    public Set<String> getKubeApiAuthorizedIpRanges() {
        return kubeApiAuthorizedIpRanges;
    }

    public void setKubeApiAuthorizedIpRanges(Set<String> kubeApiAuthorizedIpRanges) {
        this.kubeApiAuthorizedIpRanges = kubeApiAuthorizedIpRanges;
    }

    public String getOutboundType() {
        return outboundType;
    }

    public void setOutboundType(String outboundType) {
        this.outboundType = outboundType;
    }

    public Set<String> getWorkerNodeSubnetIds() {
        return workerNodeSubnetIds;
    }

    public void setWorkerNodeSubnetIds(Set<String> workerNodeSubnetIds) {
        this.workerNodeSubnetIds = workerNodeSubnetIds;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterDto{" +
                "create=" + create +
                ", privateCluster=" + privateCluster +
                ", kubeApiAuthorizedIpRanges='" + kubeApiAuthorizedIpRanges + '\'' +
                ", outboundType='" + outboundType + '\'' +
                ", workerNodeSubnetIds='" + workerNodeSubnetIds + '\'' +
                '}';
    }

    @JsonPOJOBuilder
    public static class Builder {

        private boolean create;

        private boolean privateCluster;

        private Set<String> kubeApiAuthorizedIpRanges = new HashSet<>();

        private String outboundType;

        private Set<String> workerNodeSubnetIds = new HashSet<>();

        private Builder() {
        }

        public ExternalizedComputeClusterDto.Builder withCreate(boolean create) {
            this.create = create;
            return this;
        }

        public ExternalizedComputeClusterDto.Builder withPrivateCluster(boolean privateCluster) {
            this.privateCluster = privateCluster;
            return this;
        }

        public ExternalizedComputeClusterDto.Builder withKubeApiAuthorizedIpRanges(Set<String> kubeApiAuthorizedIpRanges) {
            if (CollectionUtils.isNotEmpty(kubeApiAuthorizedIpRanges)) {
                this.kubeApiAuthorizedIpRanges = kubeApiAuthorizedIpRanges;
            }
            return this;
        }

        public ExternalizedComputeClusterDto.Builder withOutboundType(String outboundType) {
            this.outboundType = outboundType;
            return this;
        }

        public ExternalizedComputeClusterDto.Builder withWorkerNodeSubnetIds(Set<String> workerNodeSubnetIds) {
            if (CollectionUtils.isNotEmpty(workerNodeSubnetIds)) {
                this.workerNodeSubnetIds = workerNodeSubnetIds;
            }
            return this;
        }

        public ExternalizedComputeClusterDto build() {
            return new ExternalizedComputeClusterDto(this);
        }
    }
}