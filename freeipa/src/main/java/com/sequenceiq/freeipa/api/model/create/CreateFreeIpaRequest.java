package com.sequenceiq.freeipa.api.model.create;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.PLACEMENT_SETTINGS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.freeipa.api.model.credential.CredentialRequest;
import com.sequenceiq.freeipa.api.model.freeipa.FreeIpaRequest;
import com.sequenceiq.freeipa.api.model.instance.InstanceGroupV4Request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateFreeIpaRequest extends CreateFreeIpaBase implements JsonEntity {

    @Valid
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.GENERAL_SETTINGS, required = true)
    private CredentialRequest credential;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.TAGS)
    private TagsV4Request tags;

    private String owner;

    @Valid
    @ApiModelProperty(PLACEMENT_SETTINGS)
    private PlacementSettingsV4Request placement;

    @NotNull
    @Valid
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.INSTANCE_GROUPS, required = true)
    private List<InstanceGroupV4Request> instanceGroups = new ArrayList<>();

    @NotNull(message = "You should define authentication for stack!")
    @ApiModelProperty(ModelDescriptions.StackModelDescription.AUTHENTICATION)
    private StackAuthenticationV4Request authentication;

    @Valid
    @ApiModelProperty(ModelDescriptions.StackModelDescription.NETWORK)
    private NetworkV4Request network;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.IMAGE_SETTINGS)
    private ImageSettingsV4Request image;

    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.GATEWAY_PORT, allowableValues = "1025-65535")
    @Min(value = 1025, message = "Port should be between 1025 and 65535")
    @Max(value = 65535, message = "Port should be between 1025 and 65535")
    private Integer gatewayPort;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.INPUTS)
    private Map<String, Object> inputs = new HashMap<>();

    @NotNull
    private FreeIpaRequest freeIpa;

    public TagsV4Request getTags() {
        return tags;
    }

    public void setTags(TagsV4Request tags) {
        this.tags = tags;
    }

    public List<InstanceGroupV4Request> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupV4Request> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public StackAuthenticationV4Request getAuthentication() {
        return authentication;
    }

    public void setAuthentication(StackAuthenticationV4Request authentication) {
        this.authentication = authentication;
    }

    public NetworkV4Request getNetwork() {
        return network;
    }

    public void setNetwork(NetworkV4Request network) {
        this.network = network;
    }

    public ImageSettingsV4Request getImage() {
        return image;
    }

    public void setImage(ImageSettingsV4Request image) {
        this.image = image;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public PlacementSettingsV4Request getPlacement() {
        return placement;
    }

    public void setPlacement(PlacementSettingsV4Request placement) {
        this.placement = placement;
    }

    public FreeIpaRequest getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(FreeIpaRequest freeIpa) {
        this.freeIpa = freeIpa;
    }

    public CredentialRequest getCredential() {
        return credential;
    }

    public void setCredential(CredentialRequest credential) {
        this.credential = credential;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}

