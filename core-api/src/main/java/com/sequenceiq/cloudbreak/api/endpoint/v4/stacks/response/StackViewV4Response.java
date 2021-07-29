package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.UserViewV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

@JsonInclude(Include.NON_NULL)
public class StackViewV4Response implements JsonEntity {

    @ApiModelProperty(StackModelDescription.CRN)
    private String crn;

    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @ApiModelProperty(StackModelDescription.HDP_VERSION)
    private String hdpVersion;

    @ApiModelProperty(StackModelDescription.CLUSTER)
    private ClusterViewV4Response cluster;

    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    private Status status;

    @ApiModelProperty(StackModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(StackModelDescription.CREATED)
    private Long created;

    @ApiModelProperty(StackModelDescription.TERMINATED)
    private Long terminated;

    @ApiModelProperty(StackModelDescription.USER)
    private UserViewV4Response user;

    @ApiModelProperty(StackModelDescription.ENVIRONMENT_CRN)
    private String environmentCrn;

    private String environmentName;

    private String credentialName;

    @ApiModelProperty(StackModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(StackModelDescription.VARIANT)
    private String variant;

    @ApiModelProperty(StackModelDescription.TUNNEL)
    private Tunnel tunnel = Tunnel.DIRECT;

    @ApiModelProperty(StackModelDescription.STACK_VERSION)
    private String stackVersion;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public ClusterViewV4Response getCluster() {
        return cluster;
    }

    public void setCluster(ClusterViewV4Response cluster) {
        this.cluster = cluster;
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

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public UserViewV4Response getUser() {
        return user;
    }

    public void setUser(UserViewV4Response user) {
        this.user = user;
    }

    public Long getTerminated() {
        return terminated;
    }

    public void setTerminated(Long terminated) {
        this.terminated = terminated;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    @Override
    public String toString() {
        return "StackViewV4Response{" +
                "crn='" + crn + '\'' +
                ", name='" + name + '\'' +
                ", hdpVersion='" + hdpVersion + '\'' +
                ", cluster=" + cluster +
                ", status=" + status +
                ", nodeCount=" + nodeCount +
                ", created=" + created +
                ", terminated=" + terminated +
                ", user=" + user +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", environmentName='" + environmentName + '\'' +
                ", credentialName='" + credentialName + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", variant='" + variant + '\'' +
                ", tunnel=" + tunnel +
                ", stackVersion='" + stackVersion + '\'' +
                '}';
    }
}
