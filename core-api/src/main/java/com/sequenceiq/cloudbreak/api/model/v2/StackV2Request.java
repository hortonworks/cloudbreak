package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyRequest;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class StackV2Request implements JsonEntity {

    @Valid
    @NotNull
    @ApiModelProperty(StackModelDescription.GENERAL_SETTINGS)
    private GeneralSettings general;

    @Valid
    @ApiModelProperty(StackModelDescription.PLACEMENT_SETTINGS)
    private PlacementSettings placement;

    @ApiModelProperty(StackModelDescription.PLATFORM_VARIANT)
    private String platformVariant;

    @ApiModelProperty(StackModelDescription.AMBARI_VERSION)
    private String ambariVersion;

    @ApiModelProperty(StackModelDescription.HDP_VERSION)
    private String hdpVersion;

    @ApiModelProperty(StackModelDescription.PARAMETERS)
    private Map<String, ?> parameters = new HashMap<>();

    @ApiModelProperty(StackModelDescription.INPUTS)
    private Map<String, Object> inputs = new HashMap<>();

    @ApiModelProperty(StackModelDescription.CUSTOM_DOMAIN_SETTINGS)
    private CustomDomainSettings customDomain;

    @ApiModelProperty(StackModelDescription.TAGS)
    private Tags tags;

    @NotNull
    @Valid
    @ApiModelProperty(value = StackModelDescription.INSTANCE_GROUPS, required = true)
    private List<InstanceGroupV2Request> instanceGroups = new ArrayList<>();

    @ApiModelProperty(StackModelDescription.FAILURE_POLICY)
    private FailurePolicyRequest failurePolicy;

    @ApiModelProperty(StackModelDescription.AUTHENTICATION)
    private StackAuthenticationRequest stackAuthentication;

    @Valid
    @ApiModelProperty(StackModelDescription.NETWORK)
    private NetworkV2Request network;

    @ApiModelProperty(StackModelDescription.IMAGE_SETTINGS)
    private ImageSettings imageSettings;

    @ApiModelProperty(StackModelDescription.FLEX_ID)
    private Long flexId;

    @Valid
    @ApiModelProperty(StackModelDescription.CLUSTER_REQUEST)
    private ClusterV2Request cluster;

    @ApiModelProperty(StackModelDescription.GATEWAY_PORT)
    @Min(value = 1025, message = "Port should be between 1025 and 65535")
    @Max(value = 65535, message = "Port should be between 1025 and 65535")
    private Integer gatewayPort;

    public FailurePolicyRequest getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicyRequest failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public List<InstanceGroupV2Request> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupV2Request> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Long getFlexId() {
        return flexId;
    }

    public void setFlexId(Long flexId) {
        this.flexId = flexId;
    }

    public ClusterV2Request getCluster() {
        return cluster;
    }

    public void setCluster(ClusterV2Request cluster) {
        this.cluster = cluster;
    }

    public StackAuthenticationRequest getStackAuthentication() {
        return stackAuthentication;
    }

    public void setStackAuthentication(StackAuthenticationRequest stackAuthentication) {
        this.stackAuthentication = stackAuthentication;
    }

    public Map<String, ?> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, ?> parameters) {
        this.parameters = parameters;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
    }

    public String getAmbariVersion() {
        return ambariVersion;
    }

    public void setAmbariVersion(String ambariVersion) {
        this.ambariVersion = ambariVersion;
    }

    public String getHdpVersion() {
        return hdpVersion;
    }

    public void setHdpVersion(String hdpVersion) {
        this.hdpVersion = hdpVersion;
    }

    public NetworkV2Request getNetwork() {
        return network;
    }

    public void setNetwork(NetworkV2Request network) {
        this.network = network;
    }

    public CustomDomainSettings getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(CustomDomainSettings customDomain) {
        this.customDomain = customDomain;
    }

    public GeneralSettings getGeneral() {
        return general;
    }

    public void setGeneral(GeneralSettings general) {
        this.general = general;
    }

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    public ImageSettings getImageSettings() {
        return imageSettings;
    }

    public void setImageSettings(ImageSettings imageSettings) {
        this.imageSettings = imageSettings;
    }

    public PlacementSettings getPlacement() {
        return placement;
    }

    public void setPlacement(PlacementSettings placement) {
        this.placement = placement;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }
}
