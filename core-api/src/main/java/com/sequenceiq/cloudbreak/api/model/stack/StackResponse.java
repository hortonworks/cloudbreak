package com.sequenceiq.cloudbreak.api.model.stack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.CloudbreakDetailsJson;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.responses.FlexSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.model.ImageJson;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.api.model.OrchestratorResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.stack.hardware.HardwareInfoGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackResponse extends StackBase {
    @ApiModelProperty(StackModelDescription.STACK_ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount = true;

    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    private Status status;

    @ApiModelProperty(StackModelDescription.CLUSTER)
    private ClusterResponse cluster;

    @ApiModelProperty(StackModelDescription.STATUS_REASON)
    private String statusReason;

    @ApiModelProperty(StackModelDescription.CREDENTIAL)
    private CredentialV4Response credential;

    @ApiModelProperty(StackModelDescription.NETWORK)
    private NetworkResponse network;

    @Valid
    @ApiModelProperty
    private List<InstanceGroupResponse> instanceGroups = new ArrayList<>();

    @ApiModelProperty(StackModelDescription.FAILURE_POLICY)
    private FailurePolicyResponse failurePolicy;

    @ApiModelProperty(StackModelDescription.ORCHESTRATOR)
    private OrchestratorResponse orchestrator;

    @ApiModelProperty(StackModelDescription.CREATED)
    private Long created;

    @ApiModelProperty(StackModelDescription.TERMINATED)
    private Long terminated;

    @ApiModelProperty(StackModelDescription.GATEWAY_PORT)
    private Integer gatewayPort;

    @ApiModelProperty(StackModelDescription.IMAGE)
    private ImageJson image;

    @ApiModelProperty(StackModelDescription.CLOUDBREAK_DETAILS)
    private CloudbreakDetailsJson cloudbreakDetails;

    @ApiModelProperty(StackModelDescription.FLEX_SUBSCRIPTION)
    private FlexSubscriptionV4Response flexSubscription;

    @ApiModelProperty(StackModelDescription.AUTHENTICATION)
    private StackAuthenticationResponse stackAuthentication;

    @ApiModelProperty(StackModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(StackModelDescription.HARDWARE_INFO_RESPONSE)
    private Set<HardwareInfoGroupResponse> hardwareInfoGroups = new HashSet<>();

    @ApiModelProperty(StackModelDescription.EVENTS)
    private List<CloudbreakEventV4Response> cloudbreakEvents = new ArrayList<>();

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    public ClusterResponse getCluster() {
        return cluster;
    }

    public void setCluster(ClusterResponse cluster) {
        this.cluster = cluster;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<InstanceGroupResponse> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupResponse> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public OrchestratorResponse getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(OrchestratorResponse orchestrator) {
        this.orchestrator = orchestrator;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public ImageJson getImage() {
        return image;
    }

    public void setImage(ImageJson image) {
        this.image = image;
    }

    public CloudbreakDetailsJson getCloudbreakDetails() {
        return cloudbreakDetails;
    }

    public void setCloudbreakDetails(CloudbreakDetailsJson cloudbreakDetails) {
        this.cloudbreakDetails = cloudbreakDetails;
    }

    public FailurePolicyResponse getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicyResponse failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public CredentialV4Response getCredential() {
        return credential;
    }

    public void setCredential(CredentialV4Response credential) {
        this.credential = credential;
    }

    public NetworkResponse getNetwork() {
        return network;
    }

    public void setNetwork(NetworkResponse network) {
        this.network = network;
    }

    public FlexSubscriptionV4Response getFlexSubscription() {
        return flexSubscription;
    }

    public void setFlexSubscription(FlexSubscriptionV4Response flexSubscription) {
        this.flexSubscription = flexSubscription;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Set<HardwareInfoGroupResponse> getHardwareInfoGroups() {
        return hardwareInfoGroups;
    }

    public void setHardwareInfoGroups(Set<HardwareInfoGroupResponse> hardwareInfoGroups) {
        this.hardwareInfoGroups = hardwareInfoGroups;
    }

    public List<CloudbreakEventV4Response> getCloudbreakEvents() {
        return cloudbreakEvents;
    }

    public void setCloudbreakEvents(List<CloudbreakEventV4Response> cloudbreakEvents) {
        this.cloudbreakEvents = cloudbreakEvents;
    }

    public StackAuthenticationResponse getStackAuthentication() {
        return stackAuthentication;
    }

    public void setStackAuthentication(StackAuthenticationResponse stackAuthentication) {
        this.stackAuthentication = stackAuthentication;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }

    public Long getTerminated() {
        return terminated;
    }

    public void setTerminated(Long terminated) {
        this.terminated = terminated;
    }
}
