package com.sequenceiq.environment.environment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ExternalizedComputeClusterDto.Builder.class)
public class ExternalizedComputeClusterDto {

    private boolean create;

    private boolean privateCluster;

    private String kubeApiAuthorizedIpRanges;

    private String outboundType;

    private String loadBalancerAuthorizationIpRanges;

    private ExternalizedComputeClusterDto(ExternalizedComputeClusterDto.Builder builder) {
        create = builder.create;
        privateCluster = builder.privateCluster;
        kubeApiAuthorizedIpRanges = builder.kubeApiAuthorizedIpRanges;
        outboundType = builder.outboundType;
        loadBalancerAuthorizationIpRanges = builder.loadBalancerAuthorizationIpRanges;
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

    public String getKubeApiAuthorizedIpRanges() {
        return kubeApiAuthorizedIpRanges;
    }

    public void setKubeApiAuthorizedIpRanges(String kubeApiAuthorizedIpRanges) {
        this.kubeApiAuthorizedIpRanges = kubeApiAuthorizedIpRanges;
    }

    public String getOutboundType() {
        return outboundType;
    }

    public void setOutboundType(String outboundType) {
        this.outboundType = outboundType;
    }

    public String getLoadBalancerAuthorizationIpRanges() {
        return loadBalancerAuthorizationIpRanges;
    }

    public void setLoadBalancerAuthorizationIpRanges(String loadBalancerAuthorizationIpRanges) {
        this.loadBalancerAuthorizationIpRanges = loadBalancerAuthorizationIpRanges;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterDto{" +
                "create=" + create +
                ", privateCluster=" + privateCluster +
                ", kubeApiAuthorizedIpRanges='" + kubeApiAuthorizedIpRanges + '\'' +
                ", outboundType='" + outboundType + '\'' +
                ", loadBalancerAuthorizationIpRanges='" + loadBalancerAuthorizationIpRanges + '\'' +
                '}';
    }

    @JsonPOJOBuilder
    public static class Builder {

        private boolean create;

        private boolean privateCluster;

        private String kubeApiAuthorizedIpRanges;

        private String outboundType;

        private String loadBalancerAuthorizationIpRanges;

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

        public ExternalizedComputeClusterDto.Builder withKubeApiAuthorizedIpRanges(String kubeApiAuthorizedIpRanges) {
            this.kubeApiAuthorizedIpRanges = kubeApiAuthorizedIpRanges;
            return this;
        }

        public ExternalizedComputeClusterDto.Builder withOutboundType(String outboundType) {
            this.outboundType = outboundType;
            return this;
        }

        public ExternalizedComputeClusterDto.Builder withLoadBalancerAuthorizationIpRanges(String loadBalancerAuthorizationIpRanges) {
            this.loadBalancerAuthorizationIpRanges = loadBalancerAuthorizationIpRanges;
            return this;
        }

        public ExternalizedComputeClusterDto build() {
            return new ExternalizedComputeClusterDto(this);
        }
    }
}