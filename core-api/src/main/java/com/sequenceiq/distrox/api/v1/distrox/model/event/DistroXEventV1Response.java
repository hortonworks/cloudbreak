package com.sequenceiq.distrox.api.v1.distrox.model.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistroXEventV1Response implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.EventModelDescription.TYPE)
    private String eventType;

    @ApiModelProperty(ModelDescriptions.EventModelDescription.TIMESTAMP)
    private long eventTimestamp;

    @ApiModelProperty(ModelDescriptions.EventModelDescription.MESSAGE)
    private String eventMessage;

    @ApiModelProperty(ModelDescriptions.USER_ID)
    private String userId;

    @ApiModelProperty(ModelDescriptions.EventModelDescription.NOTIFICATION_TYPE)
    private String notificationType;

    @ApiModelProperty(ModelDescriptions.CLOUD_PLATFORM)
    private String cloud;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.REGION)
    private String region;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.AVAILABILITY_ZONE)
    private String availabilityZone;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @ApiModelProperty(ModelDescriptions.BlueprintModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.CLUSTER_ID)
    private Long clusterId;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.CLUSTER_NAME)
    private String clusterName;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.CRN)
    private String stackCrn;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.STACK_NAME)
    private String stackName;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.STACK_STATUS)
    private Status stackStatus;

    @ApiModelProperty(ModelDescriptions.InstanceGroupModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(ModelDescriptions.InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    private String instanceGroup;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.CLUSTER_STATUS)
    private Status clusterStatus;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_ID)
    private Long workspaceId;

    @ApiModelProperty(ModelDescriptions.TENANT_NAME)
    private String tenantName;

    private LdapDetails ldapDetails;

    private RdsDetails rdsDetails;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(long eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getEventMessage() {
        return eventMessage;
    }

    public void setEventMessage(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
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

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public Status getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(Status stackStatus) {
        this.stackStatus = stackStatus;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public Status getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(Status clusterStatus) {
        this.clusterStatus = clusterStatus;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public LdapDetails getLdapDetails() {
        return ldapDetails;
    }

    public void setLdapDetails(LdapDetails ldapDetails) {
        this.ldapDetails = ldapDetails;
    }

    public RdsDetails getRdsDetails() {
        return rdsDetails;
    }

    public void setRdsDetails(RdsDetails rdsDetails) {
        this.rdsDetails = rdsDetails;
    }
}
