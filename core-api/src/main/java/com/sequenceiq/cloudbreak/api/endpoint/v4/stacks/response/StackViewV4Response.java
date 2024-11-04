package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.UserViewV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackViewV4Response implements JsonEntity {

    @Schema(description = StackModelDescription.CRN)
    private String crn;

    @Schema(description = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @Schema(description = StackModelDescription.HDP_VERSION)
    private String hdpVersion;

    @Schema(description = StackModelDescription.CLUSTER)
    private ClusterViewV4Response cluster;

    @Schema(description = StackModelDescription.STACK_STATUS)
    private Status status;

    @Schema(description = StackModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @Schema(description = StackModelDescription.CREATED)
    private Long created;

    @Schema(description = StackModelDescription.TERMINATED)
    private Long terminated;

    @Schema(description = StackModelDescription.USER)
    private UserViewV4Response user;

    @Schema(description = StackModelDescription.ENVIRONMENT_CRN)
    private String environmentCrn;

    private String environmentName;

    private String credentialName;

    private boolean govCloud;

    @Schema(description = StackModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @Schema(description = StackModelDescription.VARIANT)
    private String variant;

    @Schema(description = StackModelDescription.TUNNEL)
    private Tunnel tunnel = Tunnel.DIRECT;

    @Schema(description = StackModelDescription.STACK_VERSION)
    private String stackVersion;

    private boolean upgradeable;

    @Schema(description = StackModelDescription.EXTERNAL_DATABASE)
    private DatabaseResponse externalDatabase;

    @Schema(description = StackModelDescription.SECURITY)
    private SecurityV4Response security;

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

    public boolean isUpgradeable() {
        return upgradeable;
    }

    public void setUpgradeable(boolean upgradeable) {
        this.upgradeable = upgradeable;
    }

    public boolean isGovCloud() {
        return govCloud;
    }

    public void setGovCloud(boolean govCloud) {
        this.govCloud = govCloud;
    }

    public DatabaseResponse getExternalDatabase() {
        return externalDatabase;
    }

    public void setExternalDatabase(DatabaseResponse externalDatabase) {
        this.externalDatabase = externalDatabase;
    }

    public SecurityV4Response getSecurityV4Response() {
        return security;
    }

    public void setSecurityV4Response(SecurityV4Response security) {
        this.security = security;
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
                ", govCloud=" + govCloud +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", variant='" + variant + '\'' +
                ", tunnel=" + tunnel +
                ", stackVersion='" + stackVersion + '\'' +
                ", upgradeable=" + upgradeable +
                ", externalDatabase=" + externalDatabase +
                ", security=" + security +
                '}';
    }
}
