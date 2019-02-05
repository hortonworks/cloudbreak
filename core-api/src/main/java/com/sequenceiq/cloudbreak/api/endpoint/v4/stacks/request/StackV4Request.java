package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.PLACEMENT_SETTINGS;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackV4Request extends StackV4Base {

    @Valid
    @NotNull
    @ApiModelProperty(value = StackModelDescription.GENERAL_SETTINGS, required = true)
    private EnvironmentSettingsV4Request environment;

    @ApiModelProperty(StackModelDescription.CUSTOM_DOMAIN_SETTINGS)
    private CustomDomainSettingsV4Request customDomain;

    @ApiModelProperty(StackModelDescription.TAGS)
    private TagsV4Request tags;

    @NotNull
    @ApiModelProperty(value = PLACEMENT_SETTINGS, required = true)
    @Valid
    private PlacementSettingsV4Request placement;

    @NotNull
    @Valid
    @ApiModelProperty(value = StackModelDescription.INSTANCE_GROUPS, required = true)
    private List<InstanceGroupV4Request> instanceGroups = new ArrayList<>();

    @NotNull(message = "You should define authentication for stack!")
    @ApiModelProperty(StackModelDescription.AUTHENTICATION)
    private StackAuthenticationV4Request authentication;

    @Valid
    @ApiModelProperty(StackModelDescription.NETWORK)
    private NetworkV4Request network;

    @ApiModelProperty(StackModelDescription.IMAGE_SETTINGS)
    private ImageSettingsV4Request image;

    @Valid
    @ApiModelProperty(StackModelDescription.CLUSTER_REQUEST)
    private ClusterV4Request cluster;

    @ApiModelProperty(value = StackModelDescription.GATEWAY_PORT, allowableValues = "1025-65535")
    @Min(value = 1025, message = "Port should be between 1025 and 65535")
    @Max(value = 65535, message = "Port should be between 1025 and 65535")
    private Integer gatewayPort;

    @ApiModelProperty(StackModelDescription.FLEX_ID)
    private Long flexId;

    private StackType type;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.SHARED_SERVICE_REQUEST)
    private SharedServiceV4Request sharedService;

    public EnvironmentSettingsV4Request getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentSettingsV4Request environment) {
        this.environment = environment;
    }

    public CustomDomainSettingsV4Request getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(CustomDomainSettingsV4Request customDomain) {
        this.customDomain = customDomain;
    }

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

    public ClusterV4Request getCluster() {
        return cluster;
    }

    public void setCluster(ClusterV4Request cluster) {
        this.cluster = cluster;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public Long getFlexId() {
        return flexId;
    }

    public void setFlexId(Long flexId) {
        this.flexId = flexId;
    }

    public StackType getType() {
        return type;
    }

    public void setType(StackType type) {
        this.type = type;
    }

    public PlacementSettingsV4Request getPlacement() {
        return placement;
    }

    public void setPlacement(PlacementSettingsV4Request placement) {
        this.placement = placement;
    }

    public SharedServiceV4Request getSharedService() {
        return sharedService;
    }

    public void setSharedService(SharedServiceV4Request sharedService) {
        this.sharedService = sharedService;
    }
}
