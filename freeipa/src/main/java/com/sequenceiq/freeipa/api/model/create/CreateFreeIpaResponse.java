package com.sequenceiq.freeipa.api.model.create;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.PLACEMENT_SETTINGS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CloudbreakDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.placement.PlacementSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateFreeIpaResponse extends CreateFreeIpaBase implements JsonEntity {
    private EnvironmentSettingsV4Response environment;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.STACK_STATUS)
    private Status status;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.STATUS_REASON)
    private String statusReason;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.NETWORK)
    private NetworkV4Response network;

    @Valid
    @ApiModelProperty
    private List<InstanceGroupV4Response> instanceGroups = new ArrayList<>();

    @ApiModelProperty(ModelDescriptions.StackModelDescription.CREATED)
    private Long created;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TERMINATED)
    private Long terminated;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.GATEWAY_PORT)
    private Integer gatewayPort;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.IMAGE)
    private StackImageV4Response image;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.CLOUDBREAK_DETAILS)
    private CloudbreakDetailsV4Response cloudbreakDetails;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.AUTHENTICATION)
    private StackAuthenticationV4Response authentication;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.HARDWARE_INFO_RESPONSE)
    private Set<HardwareInfoGroupV4Response> hardwareInfoGroups = new HashSet<>();

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TAGS)
    private TagsV4Response tags;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    @NotNull
    @ApiModelProperty(value = PLACEMENT_SETTINGS, required = true)
    @Valid
    private PlacementSettingsV4Response placement;

    public EnvironmentSettingsV4Response getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentSettingsV4Response environment) {
        this.environment = environment;
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

    public TagsV4Response getTags() {
        return tags;
    }

    public void setTags(TagsV4Response tags) {
        this.tags = tags;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }

    public PlacementSettingsV4Response getPlacement() {
        return placement;
    }

    public void setPlacement(PlacementSettingsV4Response placement) {
        this.placement = placement;
    }
}
