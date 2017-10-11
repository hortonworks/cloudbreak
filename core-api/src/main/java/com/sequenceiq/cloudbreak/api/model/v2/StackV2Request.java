package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyRequest;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackV2Request implements JsonEntity {
    @Size(max = 40, min = 5, message = "The length of the name has to be in range of 5 to 40")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @ApiModelProperty(StackModelDescription.AVAILABILITY_ZONE)
    private String availabilityZone;

    @ApiModelProperty(StackModelDescription.REGION)
    private String region;

    @ApiModelProperty(StackModelDescription.PLATFORM_VARIANT)
    private String platformVariant;

    @ApiModelProperty(StackModelDescription.FAILURE_ACTION)
    private OnFailureAction onFailureAction = OnFailureAction.DO_NOTHING;

    @ApiModelProperty(StackModelDescription.AMBARI_VERSION)
    private String ambariVersion;

    @ApiModelProperty(StackModelDescription.HDP_VERSION)
    private String hdpVersion;

    @ApiModelProperty(StackModelDescription.PARAMETERS)
    private Map<String, String> parameters = new HashMap<>();

    @ApiModelProperty(StackModelDescription.CUSTOM_DOMAIN)
    private String customDomain;

    @ApiModelProperty(StackModelDescription.CUSTOM_HOSTNAME)
    private String customHostname;

    @ApiModelProperty(StackModelDescription.CLUSTER_NAME_AS_SUBDOMAIN)
    private boolean clusterNameAsSubdomain;

    @ApiModelProperty(StackModelDescription.HOSTGROUP_NAME_AS_HOSTNAME)
    private boolean hostgroupNameAsHostname;

    @ApiModelProperty(StackModelDescription.APPLICATION_TAGS)
    private Map<String, String> applicationTags = new HashMap<>();

    @ApiModelProperty(StackModelDescription.USERDEFINED_TAGS)
    private Map<String, String> userDefinedTags = new HashMap<>();

    @ApiModelProperty(StackModelDescription.DEFAULT_TAGS)
    private Map<String, String> defaultTags = new HashMap<>();

    @Valid
    @ApiModelProperty(StackModelDescription.ORCHESTRATOR)
    private OrchestratorRequest orchestrator;

    @Valid
    @ApiModelProperty(value = StackModelDescription.INSTANCE_GROUPS, required = true)
    private List<InstanceGroupV2Request> instanceGroups = new ArrayList<>();

    @ApiModelProperty(StackModelDescription.FAILURE_POLICY)
    private FailurePolicyRequest failurePolicy;

    @ApiModelProperty(StackModelDescription.AUTHENTICATION)
    private StackAuthenticationRequest stackAuthentication;

    @ApiModelProperty(StackModelDescription.NETWORK)
    private NetworkV2Request network;

    @ApiModelProperty(StackModelDescription.IMAGE_CATALOG)
    private String imageCatalog;

    @ApiModelProperty(StackModelDescription.CUSTOM_IMAGE)
    private String customImage;

    @ApiModelProperty(StackModelDescription.FLEX_ID)
    private Long flexId;

    @ApiModelProperty(StackModelDescription.CREDENTIAL_NAME)
    private String credentialName;

    @Valid
    @ApiModelProperty(StackModelDescription.CLUSTER_REQUEST)
    private ClusterV2Request clusterRequest;

    @ApiModelProperty(hidden = true)
    private String owner;

    @ApiModelProperty(hidden = true)
    private String account;

    public FailurePolicyRequest getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicyRequest failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public OrchestratorRequest getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(OrchestratorRequest orchestrator) {
        this.orchestrator = orchestrator;
    }

    public String getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(String imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public List<InstanceGroupV2Request> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupV2Request> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public String getCustomImage() {
        return customImage;
    }

    public void setCustomImage(String customImage) {
        this.customImage = customImage;
    }

    public Long getFlexId() {
        return flexId;
    }

    public void setFlexId(Long flexId) {
        this.flexId = flexId;
    }

    public ClusterV2Request getClusterRequest() {
        return clusterRequest;
    }

    public void setClusterRequest(ClusterV2Request clusterRequest) {
        this.clusterRequest = clusterRequest;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public StackAuthenticationRequest getStackAuthentication() {
        return stackAuthentication;
    }

    public void setStackAuthentication(StackAuthenticationRequest stackAuthentication) {
        this.stackAuthentication = stackAuthentication;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public OnFailureAction getOnFailureAction() {
        return onFailureAction;
    }

    public void setOnFailureAction(OnFailureAction onFailureAction) {
        this.onFailureAction = onFailureAction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
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

    public String getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(String customDomain) {
        this.customDomain = customDomain;
    }

    public String getCustomHostname() {
        return customHostname;
    }

    public void setCustomHostname(String customHostname) {
        this.customHostname = customHostname;
    }

    public boolean isClusterNameAsSubdomain() {
        return clusterNameAsSubdomain;
    }

    public void setClusterNameAsSubdomain(boolean clusterNameAsSubdomain) {
        this.clusterNameAsSubdomain = clusterNameAsSubdomain;
    }

    public boolean isHostgroupNameAsHostname() {
        return hostgroupNameAsHostname;
    }

    public void setHostgroupNameAsHostname(boolean hostgroupNameAsHostname) {
        this.hostgroupNameAsHostname = hostgroupNameAsHostname;
    }

    public Map<String, String> getApplicationTags() {
        return applicationTags;
    }

    public void setApplicationTags(Map<String, String> applicationTags) {
        this.applicationTags = applicationTags;
    }

    public Map<String, String> getUserDefinedTags() {
        return userDefinedTags;
    }

    public void setUserDefinedTags(Map<String, String> userDefinedTags) {
        this.userDefinedTags = userDefinedTags;
    }

    public Map<String, String> getDefaultTags() {
        return defaultTags;
    }

    public void setDefaultTags(Map<String, String> defaultTags) {
        this.defaultTags = defaultTags;
    }

    public NetworkV2Request getNetwork() {
        return network;
    }

    public void setNetwork(NetworkV2Request network) {
        this.network = network;
    }
}
