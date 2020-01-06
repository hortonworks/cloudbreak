package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.PLACEMENT_SETTINGS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.customdomain.CustomDomainSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.Tunnel;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackV4Response extends StackV4Base {

    private Long id;

    private String crn;

    private String environmentCrn;

    private String environmentName;

    private String credentialName;

    private String credentialCrn;

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

    @ApiModelProperty(StackModelDescription.TELEMETRY)
    private TelemetryResponse telemetry;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    @ApiModelProperty
    private CustomDomainSettingsV4Response customDomains;

    @NotNull
    @ApiModelProperty(value = PLACEMENT_SETTINGS, required = true)
    @Valid
    private PlacementSettingsV4Response placement;

    @ApiModelProperty(ClusterModelDescription.SHARED_SERVICE_REQUEST)
    private SharedServiceV4Response sharedService;

    @ApiModelProperty(StackModelDescription.CLOUD_PLATFORM)
    private CloudPlatform cloudPlatform;

    @ApiModelProperty(StackModelDescription.TUNNEL)
    private Tunnel tunnel = Tunnel.DIRECT;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
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

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public TagsV4Response getTags() {
        return tags;
    }

    public void setTags(TagsV4Response tags) {
        this.tags = tags;
    }

    public TelemetryResponse getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(TelemetryResponse telemetry) {
        this.telemetry = telemetry;
    }

    public CustomDomainSettingsV4Response getCustomDomains() {
        return customDomains;
    }

    public void setCustomDomains(CustomDomainSettingsV4Response customDomains) {
        this.customDomains = customDomains;
    }

    public SharedServiceV4Response getSharedService() {
        return sharedService;
    }

    public void setSharedService(SharedServiceV4Response sharedService) {
        this.sharedService = sharedService;
    }

    public PlacementSettingsV4Response getPlacement() {
        return placement;
    }

    public void setPlacement(PlacementSettingsV4Response placement) {
        this.placement = placement;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public String getCredentialCrn() {
        return credentialCrn;
    }

    public void setCredentialCrn(String credentialCrn) {
        this.credentialCrn = credentialCrn;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }
}
