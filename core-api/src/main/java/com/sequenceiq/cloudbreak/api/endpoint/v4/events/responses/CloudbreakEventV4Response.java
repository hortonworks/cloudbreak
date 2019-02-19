package com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterDefinitionModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudbreakEventV4Response extends CloudbreakEventBaseV4 {

    @ApiModelProperty(ModelDescriptions.CLOUD_PLATFORM)
    private String cloud;

    @ApiModelProperty(StackModelDescription.REGION)
    private String region;

    @ApiModelProperty(StackModelDescription.AVAILABILITY_ZONE)
    private String availabilityZone;

    @ApiModelProperty(ClusterModelDescription.CLUSTER_DEFINITION_ID)
    private Long clusterDefinitionId;

    @ApiModelProperty(ClusterDefinitionModelDescription.CLUSTER_DEFINITION_NAME)
    private String clusterDefinitionName;

    @ApiModelProperty(ClusterModelDescription.CLUSTER_ID)
    private Long clusterId;

    @ApiModelProperty(ClusterModelDescription.CLUSTER_NAME)
    private String clusterName;

    @ApiModelProperty(StackModelDescription.STACK_ID)
    private Long stackId;

    @ApiModelProperty(StackModelDescription.STACK_NAME)
    private String stackName;

    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    private Status stackStatus;

    @ApiModelProperty(InstanceGroupModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    private String instanceGroup;

    @ApiModelProperty(StackModelDescription.CLUSTER_STATUS)
    private Status clusterStatus;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_ID)
    private Long workspaceId;

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

    public String getClusterDefinitionName() {
        return clusterDefinitionName;
    }

    public void setClusterDefinitionName(String clusterDefinitionName) {
        this.clusterDefinitionName = clusterDefinitionName;
    }

    public Long getClusterDefinitionId() {
        return clusterDefinitionId;
    }

    public void setClusterDefinitionId(Long clusterDefinitionId) {
        this.clusterDefinitionId = clusterDefinitionId;
    }

    public Status getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(Status stackStatus) {
        this.stackStatus = stackStatus;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
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
}
