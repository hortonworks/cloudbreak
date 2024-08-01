package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExternalizedComputeClusterV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalizedComputeClusterResponse {

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_PRIVATE_CLUSTER)
    private boolean privateCluster;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_KUBE_API_AUTHORIZED_IP_RANGES)
    private Set<String> kubeApiAuthorizedIpRanges;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_OUTBOUND_TYPE)
    private String outboundType;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_WORKER_NODE_SUBNET_IDS)
    private Set<String> workerNodeSubnetIds;

    public static ExternalizedComputeClusterResponse.Builder newBuilder() {
        return new Builder();
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
        return "ExternalizedComputeClusterResponse{" +
                "privateCluster=" + privateCluster +
                ", kubeApiAuthorizedIpRanges='" + kubeApiAuthorizedIpRanges + '\'' +
                ", outboundType='" + outboundType + '\'' +
                ", workerNodeSubnetIds='" + workerNodeSubnetIds + '\'' +
                '}';
    }

    public static class Builder {

        private boolean privateCluster;

        private Set<String> kubeApiAuthorizedIpRanges = new HashSet<>();

        private String outboundType;

        private Set<String> workerNodeSubnetIds;

        public Builder withPrivateCluster(boolean privateCluster) {
            this.privateCluster = privateCluster;
            return this;
        }

        public Builder withKubeApiAuthorizedIpRanges(Set<String> kubeApiAuthorizedIpRanges) {
            if (CollectionUtils.isNotEmpty(kubeApiAuthorizedIpRanges)) {
                this.kubeApiAuthorizedIpRanges = kubeApiAuthorizedIpRanges;
            }
            return this;
        }

        public Builder withOutboundType(String outboundType) {
            this.outboundType = outboundType;
            return this;
        }

        public Builder withWorkerNodeSubnetIds(Set<String> workerNodeSubnetIds) {
            this.workerNodeSubnetIds = workerNodeSubnetIds;
            return this;
        }

        public ExternalizedComputeClusterResponse build() {
            ExternalizedComputeClusterResponse externalizedComputeCluster = new ExternalizedComputeClusterResponse();
            externalizedComputeCluster.setPrivateCluster(privateCluster);
            externalizedComputeCluster.setOutboundType(outboundType);
            externalizedComputeCluster.setKubeApiAuthorizedIpRanges(kubeApiAuthorizedIpRanges);
            externalizedComputeCluster.setWorkerNodeSubnetIds(workerNodeSubnetIds);
            return externalizedComputeCluster;
        }
    }

}
