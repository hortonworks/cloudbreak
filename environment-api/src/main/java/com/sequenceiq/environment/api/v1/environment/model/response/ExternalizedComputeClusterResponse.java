package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExternalizedComputeClusterV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalizedComputeClusterResponse {

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_PRIVATE_CLUSTER)
    private boolean privateCluster;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_KUBE_API_AUTHORIZED_IP_RANGES)
    private String kubeApiAuthorizedIpRanges;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_OUTBOUND_TYPE)
    private String outboundType;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_LOAD_BALANCER_AUTHORIZED_IP_RANGES)
    private String loadBalancerAuthorizedIpRanges;

    public static ExternalizedComputeClusterResponse.Builder newBuilder() {
        return new Builder();
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

    public String getLoadBalancerAuthorizedIpRanges() {
        return loadBalancerAuthorizedIpRanges;
    }

    public void setLoadBalancerAuthorizedIpRanges(String loadBalancerAuthorizedIpRanges) {
        this.loadBalancerAuthorizedIpRanges = loadBalancerAuthorizedIpRanges;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterResponse{" +
                "privateCluster=" + privateCluster +
                ", kubeApiAuthorizedIpRanges='" + kubeApiAuthorizedIpRanges + '\'' +
                ", outboundType='" + outboundType + '\'' +
                ", loadBalancerAuthorizedIpRanges='" + loadBalancerAuthorizedIpRanges + '\'' +
                '}';
    }

    public static class Builder {

        private boolean privateCluster;

        private String kubeApiAuthorizedIpRanges;

        private String outboundType;

        private String loadBalancerAuthorizedIpRanges;

        public Builder withPrivateCluster(boolean privateCluster) {
            this.privateCluster = privateCluster;
            return this;
        }

        public Builder withKubeApiAuthorizedIpRanges(String kubeApiAuthorizedIpRanges) {
            this.kubeApiAuthorizedIpRanges = kubeApiAuthorizedIpRanges;
            return this;
        }

        public Builder withOutboundType(String outboundType) {
            this.outboundType = outboundType;
            return this;
        }

        public Builder withLoadBalancerAuthorizedIpRanges(String loadBalancerAuthorizedIpRanges) {
            this.loadBalancerAuthorizedIpRanges = loadBalancerAuthorizedIpRanges;
            return this;
        }

        public ExternalizedComputeClusterResponse build() {
            ExternalizedComputeClusterResponse externalizedComputeCluster = new ExternalizedComputeClusterResponse();
            externalizedComputeCluster.setPrivateCluster(privateCluster);
            externalizedComputeCluster.setOutboundType(outboundType);
            externalizedComputeCluster.setKubeApiAuthorizedIpRanges(kubeApiAuthorizedIpRanges);
            externalizedComputeCluster.setLoadBalancerAuthorizedIpRanges(loadBalancerAuthorizedIpRanges);
            return externalizedComputeCluster;
        }
    }

}
