package com.sequenceiq.cloudbreak.structuredevent.controller.events.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class CDPEventV4Response extends CDPEventBaseV4 {

    private String cloud;

    private String region;

    private String availabilityZone;

    private Long blueprintId;

    private String blueprintName;

    private Long clusterId;

    private String clusterName;

    private String stackCrn;

    private String stackName;

    private String stackStatus;

    private Integer nodeCount;

    private String instanceGroup;

    private String clusterStatus;

    private Long workspaceId;

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

    public String getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(String stackStatus) {
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

    public String getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(String clusterStatus) {
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
