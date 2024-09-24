package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.AzureExternalizedComputeParams;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExternalizedComputeClusterV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalizedComputeClusterResponse {

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_PRIVATE_CLUSTER)
    private boolean privateCluster;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_KUBE_API_AUTHORIZED_IP_RANGES)
    private Set<String> kubeApiAuthorizedIpRanges;

    /**
     * @deprecated Use provider-specific azure.outboundType field instead
     */
    @Deprecated(forRemoval = true)
    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_OUTBOUND_TYPE)
    private String outboundType;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_WORKER_NODE_SUBNET_IDS)
    private Set<String> workerNodeSubnetIds;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_AZURE_PARAMS)
    private AzureExternalizedComputeParams azure;

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

    /**
     * @deprecated Use provider-specific azure.outboundType field instead
     */
    @Deprecated(forRemoval = true)
    public String getOutboundType() {
        return outboundType;
    }

    /**
     * @deprecated Use provider-specific azure.outboundType field instead
     */
    @Deprecated(forRemoval = true)
    public void setOutboundType(String outboundType) {
        this.outboundType = outboundType;
    }

    public Set<String> getWorkerNodeSubnetIds() {
        return workerNodeSubnetIds;
    }

    public void setWorkerNodeSubnetIds(Set<String> workerNodeSubnetIds) {
        this.workerNodeSubnetIds = workerNodeSubnetIds;
    }

    public AzureExternalizedComputeParams getAzure() {
        return azure;
    }

    public void setAzure(AzureExternalizedComputeParams azure) {
        this.azure = azure;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterResponse{" +
                "privateCluster=" + privateCluster +
                ", kubeApiAuthorizedIpRanges='" + kubeApiAuthorizedIpRanges + '\'' +
                ", outboundType='" + outboundType + '\'' +
                ", workerNodeSubnetIds='" + workerNodeSubnetIds + '\'' +
                ", azure='" + azure + '\'' +
                '}';
    }

    public static class Builder {

        private boolean privateCluster;

        private Set<String> kubeApiAuthorizedIpRanges = new HashSet<>();

        private String outboundType;

        private Set<String> workerNodeSubnetIds;

        private AzureExternalizedComputeParams azure;

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

        /**
         * @deprecated Use provider-specific azure.outboundType field instead
         */
        @Deprecated(forRemoval = true)
        public Builder withOutboundType(String outboundType) {
            this.outboundType = outboundType;
            return this;
        }

        public Builder withWorkerNodeSubnetIds(Set<String> workerNodeSubnetIds) {
            this.workerNodeSubnetIds = workerNodeSubnetIds;
            return this;
        }

        public Builder withAzure(AzureExternalizedComputeParams azure) {
            this.azure = azure;
            return this;
        }

        public ExternalizedComputeClusterResponse build() {
            ExternalizedComputeClusterResponse externalizedComputeCluster = new ExternalizedComputeClusterResponse();
            externalizedComputeCluster.setPrivateCluster(privateCluster);
            externalizedComputeCluster.setOutboundType(outboundType);
            externalizedComputeCluster.setKubeApiAuthorizedIpRanges(kubeApiAuthorizedIpRanges);
            externalizedComputeCluster.setWorkerNodeSubnetIds(workerNodeSubnetIds);
            externalizedComputeCluster.setAzure(azure);
            return externalizedComputeCluster;
        }
    }

}
