package com.sequenceiq.freeipa.api.model.freeipa;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.PLACEMENT_SETTINGS;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CloudbreakDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.placement.PlacementSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIpaDetailsResponse extends FreeIpaBase implements JsonEntity {
    private EnvironmentSettingsV4Response environment;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.STACK_STATUS)
    private Status status;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.STATUS_REASON)
    private String statusReason;

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

    @ApiModelProperty(ModelDescriptions.StackModelDescription.NODE_COUNT)
    private Integer nodeCount;

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

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
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
