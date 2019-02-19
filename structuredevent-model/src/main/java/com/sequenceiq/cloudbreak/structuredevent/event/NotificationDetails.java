package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationDetails implements Serializable {
    private String notificationType;

    private String notification;

    private String cloud;

    private String region;

    private String availabiltyZone;

    private Long stackId;

    private String stackName;

    private String stackStatus;

    private Integer nodeCount;

    private String instanceGroup;

    private Long clusterId;

    private String clusterName;

    private String clusterStatus;

    private String clusterDefinitionName;

    private Long clusterDefinitionId;

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
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

    public String getAvailabiltyZone() {
        return availabiltyZone;
    }

    public void setAvailabiltyZone(String availabiltyZone) {
        this.availabiltyZone = availabiltyZone;
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

    public String getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(String stackStatus) {
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

    public String getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(String clusterStatus) {
        this.clusterStatus = clusterStatus;
    }
}
