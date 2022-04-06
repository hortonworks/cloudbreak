package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;
import com.sequenceiq.common.api.type.Tunnel;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoscaleStackV4Response {

    @ApiModelProperty(StackModelDescription.TENANANT)
    private String tenant;

    @ApiModelProperty(StackModelDescription.WORKSPACE_ID)
    private Long workspaceId;

    @ApiModelProperty(StackModelDescription.USER_ID)
    private String userId;

    @ApiModelProperty(StackModelDescription.USER_CRN)
    private String userCrn;

    @ApiModelProperty(StackModelDescription.STACK_ID)
    private Long stackId;

    @ValidStackNameFormat
    @ValidStackNameLength
    @NotNull
    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @ApiModelProperty(StackModelDescription.GATEWAY_PORT)
    private Integer gatewayPort;

    @ApiModelProperty(StackModelDescription.SERVER_IP)
    private String clusterManagerIp;

    @ApiModelProperty(StackModelDescription.USERNAME)
    private String userNamePath;

    @ApiModelProperty(StackModelDescription.PASSWORD)
    private String passwordPath;

    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    private Status status;

    @ApiModelProperty(ClusterModelDescription.STATUS)
    private Status clusterStatus;

    @ApiModelProperty(StackModelDescription.CREATED)
    private Long created;

    @ApiModelProperty(ClusterModelDescription.VARIANT)
    private String clusterManagerVariant;

    @ApiModelProperty(StackModelDescription.CRN)
    private String stackCrn;

    @ApiModelProperty(StackModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(StackModelDescription.TYPE)
    private StackType stackType;

    @ApiModelProperty(StackModelDescription.ENVIRONMENT_CRN)
    private String environmentCrn;

    @ApiModelProperty(StackModelDescription.TUNNEL)
    private Tunnel tunnel;

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public String getClusterManagerIp() {
        return clusterManagerIp;
    }

    public void setClusterManagerIp(String clusterManagerIp) {
        this.clusterManagerIp = clusterManagerIp;
    }

    public String getUserNamePath() {
        return userNamePath;
    }

    public void setUserNamePath(String userNamePath) {
        this.userNamePath = userNamePath;
    }

    public String getPasswordPath() {
        return passwordPath;
    }

    public void setPasswordPath(String passwordPath) {
        this.passwordPath = passwordPath;
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

    public Status getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(Status clusterStatus) {
        this.clusterStatus = clusterStatus;
    }

    public String getClusterManagerVariant() {
        return clusterManagerVariant;
    }

    public void setClusterManagerVariant(String clusterManagerVariant) {
        this.clusterManagerVariant = clusterManagerVariant;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public StackType getStackType() {
        return stackType;
    }

    public void setStackType(StackType stackType) {
        this.stackType = stackType;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public void setUserCrn(String userCrn) {
        this.userCrn = userCrn;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }
}
