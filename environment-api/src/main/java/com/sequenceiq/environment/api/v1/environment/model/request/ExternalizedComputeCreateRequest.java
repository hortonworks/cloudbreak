package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.AzureExternalizedComputeParams;
import com.sequenceiq.environment.api.v1.environment.validator.cidr.ValidCidrListAsString;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ExternalizedComputeRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalizedComputeCreateRequest implements Serializable {

    @NotNull
    @Schema(description = EnvironmentModelDescription.CREATE_EXTERNALIZED_COMPUTE_CLUSTER)
    private boolean create;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_PRIVATE_CLUSTER)
    private boolean privateCluster;

    @ValidCidrListAsString
    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_KUBE_API_AUTHORIZED_IP_RANGES)
    private String kubeApiAuthorizedIpRanges;

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
        return "ExternalizedComputeCreateRequest{" +
                "create=" + create +
                ", privateCluster=" + privateCluster +
                ", kubeApiAuthorizedIpRanges='" + kubeApiAuthorizedIpRanges + '\'' +
                ", outboundType='" + outboundType + '\'' +
                ", workerNodeSubnetIds='" + workerNodeSubnetIds + '\'' +
                ", azure='" + azure + '\'' +
                '}';
    }
}
