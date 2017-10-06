package com.sequenceiq.cloudbreak.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRequest extends StackBase {
    @Valid
    @ApiModelProperty(StackModelDescription.ORCHESTRATOR)
    private OrchestratorRequest orchestrator;

    @Valid
    @ApiModelProperty(value = StackModelDescription.INSTANCE_GROUPS, required = true)
    private List<InstanceGroupRequest> instanceGroups = new ArrayList<>();

    @ApiModelProperty(StackModelDescription.FAILURE_POLICY)
    private FailurePolicyRequest failurePolicy;

    @ApiModelProperty(StackModelDescription.CREDENTIAL)
    private CredentialRequest credential;

    @ApiModelProperty(StackModelDescription.AUTHENTICATION)
    private StackAuthenticationRequest stackAuthentication;

    @ApiModelProperty(StackModelDescription.NETWORK)
    private NetworkRequest network;

    @ApiModelProperty(StackModelDescription.IMAGE_CATALOG)
    private String imageCatalog;

    @ApiModelProperty(StackModelDescription.CUSTOM_IMAGE)
    private String customImage;

    @ApiModelProperty(StackModelDescription.FLEX_ID)
    private Long flexId;

    @ApiModelProperty(StackModelDescription.SOURCE_CREDENTIAL)
    private CredentialSourceRequest credentialSource;

    @ApiModelProperty(StackModelDescription.CREDENTIAL_NAME)
    private String credentialName;

    @Valid
    @ApiModelProperty(StackModelDescription.CLUSTER_REQUEST)
    private ClusterRequest clusterRequest;

    @ApiModelProperty(hidden = true)
    private String owner;

    @ApiModelProperty(hidden = true)
    private String account;

    public FailurePolicyRequest getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicyRequest failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public OrchestratorRequest getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(OrchestratorRequest orchestrator) {
        this.orchestrator = orchestrator;
    }

    public String getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(String imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public List<InstanceGroupRequest> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupRequest> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public CredentialRequest getCredential() {
        return credential;
    }

    public void setCredential(CredentialRequest credential) {
        this.credential = credential;
    }

    public NetworkRequest getNetwork() {
        return network;
    }

    public void setNetwork(NetworkRequest network) {
        this.network = network;
    }

    public String getCustomImage() {
        return customImage;
    }

    public void setCustomImage(String customImage) {
        this.customImage = customImage;
    }

    public Long getFlexId() {
        return flexId;
    }

    public void setFlexId(Long flexId) {
        this.flexId = flexId;
    }

    public CredentialSourceRequest getCredentialSource() {
        return credentialSource;
    }

    public void setCredentialSource(CredentialSourceRequest credentialSource) {
        this.credentialSource = credentialSource;
    }

    public ClusterRequest getClusterRequest() {
        return clusterRequest;
    }

    public void setClusterRequest(ClusterRequest clusterRequest) {
        this.clusterRequest = clusterRequest;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public StackAuthenticationRequest getStackAuthentication() {
        return stackAuthentication;
    }

    public void setStackAuthentication(StackAuthenticationRequest stackAuthentication) {
        this.stackAuthentication = stackAuthentication;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }
}
