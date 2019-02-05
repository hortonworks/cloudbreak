package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.responses.FlexSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.customdomain.CustomDomainSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackV4Response extends StackV4Base {

    @ApiModelProperty(StackModelDescription.STACK_ID)
    private Long id;

    private EnvironmentSettingsV4Response environment;

    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    private Status status;

    @ApiModelProperty(StackModelDescription.CLUSTER)
    private ClusterV4Response cluster;

    @ApiModelProperty(StackModelDescription.STATUS_REASON)
    private String statusReason;

    @ApiModelProperty(StackModelDescription.NETWORK)
    private NetworkV4Response network;

    @Valid
    @ApiModelProperty
    private List<InstanceGroupV4Response> instanceGroups = new ArrayList<>();

    @ApiModelProperty(StackModelDescription.CREATED)
    private Long created;

    @ApiModelProperty(StackModelDescription.TERMINATED)
    private Long terminated;

    @ApiModelProperty(StackModelDescription.GATEWAY_PORT)
    private Integer gatewayPort;

    @ApiModelProperty(StackModelDescription.IMAGE)
    private StackImageV4Response image;

    @ApiModelProperty(StackModelDescription.CLOUDBREAK_DETAILS)
    private CloudbreakDetailsV4Response cloudbreakDetails;

    @ApiModelProperty(StackModelDescription.FLEX_SUBSCRIPTION)
    private FlexSubscriptionV4Response flexSubscription;

    @ApiModelProperty(StackModelDescription.AUTHENTICATION)
    private StackAuthenticationV4Response authentication;

    @ApiModelProperty(StackModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(StackModelDescription.HARDWARE_INFO_RESPONSE)
    private Set<HardwareInfoGroupV4Response> hardwareInfoGroups = new HashSet<>();

    @ApiModelProperty(StackModelDescription.EVENTS)
    private List<CloudbreakEventV4Response> cloudbreakEvents = new ArrayList<>();

    @ApiModelProperty(StackModelDescription.TAGS)
    private TagsV4Response tags;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    @ApiModelProperty
    private CustomDomainSettingsV4Response customDomains;

    private CloudPlatform cloudPlatform;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.SHARED_SERVICE_REQUEST)
    private SharedServiceV4Response sharedService;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ClusterV4Response getCluster() {
        return cluster;
    }

    public void setCluster(ClusterV4Response cluster) {
        this.cluster = cluster;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public NetworkV4Response getNetwork() {
        return network;
    }

    public void setNetwork(NetworkV4Response network) {
        this.network = network;
    }

    public List<InstanceGroupV4Response> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupV4Response> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getTerminated() {
        return terminated;
    }

    public void setTerminated(Long terminated) {
        this.terminated = terminated;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public StackImageV4Response getImage() {
        return image;
    }

    public void setImage(StackImageV4Response image) {
        this.image = image;
    }

    public CloudbreakDetailsV4Response getCloudbreakDetails() {
        return cloudbreakDetails;
    }

    public void setCloudbreakDetails(CloudbreakDetailsV4Response cloudbreakDetails) {
        this.cloudbreakDetails = cloudbreakDetails;
    }

    public FlexSubscriptionV4Response getFlexSubscription() {
        return flexSubscription;
    }

    public void setFlexSubscription(FlexSubscriptionV4Response flexSubscription) {
        this.flexSubscription = flexSubscription;
    }

    public StackAuthenticationV4Response getAuthentication() {
        return authentication;
    }

    public void setAuthentication(StackAuthenticationV4Response authentication) {
        this.authentication = authentication;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Set<HardwareInfoGroupV4Response> getHardwareInfoGroups() {
        return hardwareInfoGroups;
    }

    public void setHardwareInfoGroups(Set<HardwareInfoGroupV4Response> hardwareInfoGroups) {
        this.hardwareInfoGroups = hardwareInfoGroups;
    }

    public List<CloudbreakEventV4Response> getCloudbreakEvents() {
        return cloudbreakEvents;
    }

    public void setCloudbreakEvents(List<CloudbreakEventV4Response> cloudbreakEvents) {
        this.cloudbreakEvents = cloudbreakEvents;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }

    public EnvironmentSettingsV4Response getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentSettingsV4Response environment) {
        this.environment = environment;
    }

    public TagsV4Response getTags() {
        return tags;
    }

    public void setTags(TagsV4Response tags) {
        this.tags = tags;
    }

    public CustomDomainSettingsV4Response getCustomDomains() {
        return customDomains;
    }

    public void setCustomDomains(CustomDomainSettingsV4Response customDomains) {
        this.customDomains = customDomains;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    @Override
    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public SharedServiceV4Response getSharedService() {
        return sharedService;
    }

    public void setSharedService(SharedServiceV4Response sharedService) {
        this.sharedService = sharedService;
    }
}
