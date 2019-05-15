package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("DescribeFreeIpaV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DescribeFreeIpaResponse {
    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.ENVIRONMENT_ID, required = true)
    private String environmentId;

    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.FREEIPA_NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(FreeIpaModelDescriptions.PLACEMENT_SETTINGS)
    private PlacementResponse placement;

    @NotNull
    @Valid
    @ApiModelProperty(value = FreeIpaModelDescriptions.INSTANCE_GROUPS, required = true)
    private List<InstanceGroupResponse> instanceGroups;

    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.AUTHENTICATION, required = true)
    private StackAuthenticationResponse authentication;

    @Valid
    @ApiModelProperty(FreeIpaModelDescriptions.NETWORK)
    private NetworkResponse network;

    @ApiModelProperty(FreeIpaModelDescriptions.IMAGE_SETTINGS)
    private ImageSettingsResponse image;

    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.FREEIPA_SERVER_SETTINGS, required = true)
    private FreeIpaServerResponse freeIpa;

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlacementResponse getPlacement() {
        return placement;
    }

    public void setPlacement(PlacementResponse placement) {
        this.placement = placement;
    }

    public List<InstanceGroupResponse> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupResponse> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public StackAuthenticationResponse getAuthentication() {
        return authentication;
    }

    public void setAuthentication(StackAuthenticationResponse authentication) {
        this.authentication = authentication;
    }

    public NetworkResponse getNetwork() {
        return network;
    }

    public void setNetwork(NetworkResponse network) {
        this.network = network;
    }

    public ImageSettingsResponse getImage() {
        return image;
    }

    public void setImage(ImageSettingsResponse image) {
        this.image = image;
    }

    public FreeIpaServerResponse getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(FreeIpaServerResponse freeIpa) {
        this.freeIpa = freeIpa;
    }
}
