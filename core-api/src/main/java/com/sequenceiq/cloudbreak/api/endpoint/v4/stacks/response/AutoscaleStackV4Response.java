package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;
import com.sequenceiq.common.api.type.Tunnel;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoscaleStackV4Response {

    @Schema(description = StackModelDescription.TENANANT)
    private String tenant;

    @Schema(description = StackModelDescription.WORKSPACE_ID)
    private Long workspaceId;

    @Schema(description = StackModelDescription.USER_ID)
    private String userId;

    @Schema(description = StackModelDescription.USER_CRN)
    private String userCrn;

    @Schema(description = StackModelDescription.STACK_ID)
    private Long stackId;

    @ValidStackNameFormat
    @ValidStackNameLength
    @NotNull
    @Schema(description = StackModelDescription.STACK_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = StackModelDescription.GATEWAY_PORT)
    private Integer gatewayPort;

    @Schema(description = StackModelDescription.SERVER_IP)
    private String clusterManagerIp;

    @Schema(description = StackModelDescription.USERNAME)
    private String userNamePath;

    @Schema(description = StackModelDescription.PASSWORD)
    private String passwordPath;

    @Schema(description = StackModelDescription.STACK_STATUS)
    private Status status;

    @Schema(description = ClusterModelDescription.STATUS)
    private Status clusterStatus;

    @Schema(description = StackModelDescription.CREATED)
    private Long created;

    @Schema(description = ClusterModelDescription.VARIANT)
    private String clusterManagerVariant;

    @Schema(description = ClusterModelDescription.BLUEPRINT)
    private String bluePrintText;

    @Schema(description = StackModelDescription.CRN)
    private String stackCrn;

    @Schema(description = StackModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @Schema(description = StackModelDescription.TYPE)
    private StackType stackType;

    @Schema(description = StackModelDescription.ENVIRONMENT_CRN)
    private String environmentCrn;

    @Schema(description = StackModelDescription.TUNNEL)
    private Tunnel tunnel;

    @Schema(description = StackModelDescription.SALT_CB_VERSION)
    private String saltCbVersion;

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }

    public String getBluePrintText() {
        return bluePrintText;
    }

    public void setBluePrintText(String bluePrintText) {
        this.bluePrintText = bluePrintText;
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

    public String getSaltCbVersion() {
        return saltCbVersion;
    }

    public void setSaltCbVersion(String saltCbVersion) {
        this.saltCbVersion = saltCbVersion;
    }
}
