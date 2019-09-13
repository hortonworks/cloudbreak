package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.authorization.api.EnvironmentCrnAwareApiModel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CreateFreeIpaV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateFreeIpaRequest implements EnvironmentCrnAwareApiModel {
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.FREEIPA_NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(FreeIpaModelDescriptions.PLACEMENT_SETTINGS)
    private PlacementRequest placement;

    @Valid
    @ApiModelProperty(value = FreeIpaModelDescriptions.INSTANCE_GROUPS, required = true)
    private List<InstanceGroupRequest> instanceGroups;

    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.AUTHENTICATION, required = true)
    private StackAuthenticationRequest authentication;

    @Valid
    @ApiModelProperty(FreeIpaModelDescriptions.NETWORK)
    private NetworkRequest network;

    @ApiModelProperty(FreeIpaModelDescriptions.IMAGE_SETTINGS)
    private ImageSettingsRequest image;

    @NotNull
    @Valid
    @ApiModelProperty(value = FreeIpaModelDescriptions.FREEIPA_SERVER_SETTINGS, required = true)
    private FreeIpaServerRequest freeIpa;

    @ApiModelProperty(value = FreeIpaModelDescriptions.GATEWAY_PORT, allowableValues = "1025-65535")
    @Min(value = 1025, message = "Port should be between 1025 and 65535")
    @Max(value = 65535, message = "Port should be between 1025 and 65535")
    private Integer gatewayPort;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlacementRequest getPlacement() {
        return placement;
    }

    public void setPlacement(PlacementRequest placement) {
        this.placement = placement;
    }

    public List<InstanceGroupRequest> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupRequest> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public StackAuthenticationRequest getAuthentication() {
        return authentication;
    }

    public void setAuthentication(StackAuthenticationRequest authentication) {
        this.authentication = authentication;
    }

    public NetworkRequest getNetwork() {
        return network;
    }

    public void setNetwork(NetworkRequest network) {
        this.network = network;
    }

    public ImageSettingsRequest getImage() {
        return image;
    }

    public void setImage(ImageSettingsRequest image) {
        this.image = image;
    }

    public FreeIpaServerRequest getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(FreeIpaServerRequest freeIpa) {
        this.freeIpa = freeIpa;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    @Override
    public String toString() {
        return "CreateFreeIpaRequest{"
                + "environmentCrn='" + environmentCrn + '\''
                + ", name='" + name + '\''
                + ", placement=" + placement
                + ", instanceGroups=" + instanceGroups
                + ", authentication=" + authentication
                + ", network=" + network
                + ", image=" + image
                + ", freeIpa=" + freeIpa
                + '}';
    }
}
