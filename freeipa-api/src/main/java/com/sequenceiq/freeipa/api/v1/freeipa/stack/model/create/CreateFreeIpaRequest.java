package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.StackAuthenticationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.credential.CredentialRequest;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("CreateFreeIpaV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateFreeIpaRequest {
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
    @ApiModelProperty(value = FreeIpaModelDescriptions.FREEIPA_SERVER_SETTINGS, required = true)
    private FreeIpaServerRequest freeIpa;

    //Until Environment service is ready
    @NotNull
    @ApiModelProperty(value = "Credential", required = true)
    private CredentialRequest credential;

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

    public CredentialRequest getCredential() {
        return credential;
    }

    public void setCredential(CredentialRequest credential) {
        this.credential = credential;
    }
}
