package com.sequenceiq.cloudbreak.api.model.stack;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.STACK_ID;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterViewResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class StackViewResponse {
    @ApiModelProperty(STACK_ID)
    private Long id;

    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @ApiModelProperty(ModelDescriptions.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(StackModelDescription.PLATFORM_VARIANT)
    private String platformVariant;

    @ApiModelProperty(StackModelDescription.CREDENTIAL)
    private CredentialResponse credential;

    @ApiModelProperty(StackModelDescription.HDP_VERSION)
    private String hdpVersion;

    @ApiModelProperty(StackModelDescription.CLUSTER)
    private ClusterViewResponse cluster;

    @ApiModelProperty(StackModelDescription.PARAMETERS)
    private Map<String, String> parameters = new HashMap<>();

    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    private Status status;

    @ApiModelProperty(StackModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(StackModelDescription.CREATED)
    private Long created;

    @ApiModelProperty(StackModelDescription.FLEX_SUBSCRIPTION)
    private FlexSubscriptionResponse flexSubscription;

    @ApiModelProperty(StackModelDescription.USER)
    private UserViewResponse user;

    @ApiModelProperty(StackModelDescription.ENVIRONMENT)
    private String environment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClusterViewResponse getCluster() {
        return cluster;
    }

    public void setCluster(ClusterViewResponse cluster) {
        this.cluster = cluster;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
    }

    public CredentialResponse getCredential() {
        return credential;
    }

    public void setCredential(CredentialResponse credential) {
        this.credential = credential;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getHdpVersion() {
        return hdpVersion;
    }

    public void setHdpVersion(String hdpVersion) {
        this.hdpVersion = hdpVersion;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public FlexSubscriptionResponse getFlexSubscription() {
        return flexSubscription;
    }

    public void setFlexSubscription(FlexSubscriptionResponse flexSubscription) {
        this.flexSubscription = flexSubscription;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public UserViewResponse getUser() {
        return user;
    }

    public void setUser(UserViewResponse user) {
        this.user = user;
    }
}
