package com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(Include.NON_NULL)
public class CloudbreakEventV4Response extends CloudbreakEventBaseV4 {

    @Schema(description = ModelDescriptions.CLOUD_PLATFORM)
    private String cloud;

    @Schema(description = StackModelDescription.REGION)
    private String region;

    @Schema(description = StackModelDescription.AVAILABILITY_ZONE)
    private String availabilityZone;

    @Schema(description = ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @Schema(description = BlueprintModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @Schema(description = ClusterModelDescription.CLUSTER_ID)
    private Long clusterId;

    @Schema(description = ClusterModelDescription.CLUSTER_NAME)
    private String clusterName;

    @Schema(description = StackModelDescription.CRN)
    private String stackCrn;

    @Schema(description = StackModelDescription.STACK_NAME)
    private String stackName;

    @Schema(description = StackModelDescription.STACK_STATUS)
    private Status stackStatus;

    @Schema(description = InstanceGroupModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @Schema(description = InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    private String instanceGroup;

    @Schema(description = StackModelDescription.CLUSTER_STATUS)
    private Status clusterStatus;

    @Schema(description = ModelDescriptions.WORKSPACE_ID)
    private Long workspaceId;

    @Schema(description = ModelDescriptions.TENANT_NAME)
    private String tenantName;

    private LdapDetails ldapDetails;

    private RdsDetails rdsDetails;

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
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

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public Status getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(Status stackStatus) {
        this.stackStatus = stackStatus;
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

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
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

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
}
