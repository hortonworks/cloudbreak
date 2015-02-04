package com.sequenceiq.cloudbreak.service.notification;

import java.util.Date;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.Status;

public class Notification {

    private String eventType;
    private Date eventTimestamp;
    private String eventMessage;
    private String owner;
    private String account;
    private String cloud;
    private String region;
    private String blueprintName;
    private long blueprintId;
    private Long stackId;
    private String stackName;
    private Status stackStatus;
    private Integer nodeCount;
    private String instanceGroup;

    public Notification() {
    }

    public Notification(CloudbreakEvent event) {
        this.eventType = event.getEventType();
        this.eventTimestamp = event.getEventTimestamp();
        this.eventMessage = event.getEventMessage();
        this.owner = event.getOwner();
        this.account = event.getAccount();
        this.cloud = event.getCloud();
        this.region = event.getRegion();
        this.blueprintName = event.getBlueprintName();
        this.blueprintId = event.getBlueprintId();
        this.stackId = event.getStackId();
        this.stackName = event.getStackName();
        this.stackStatus = event.getStackStatus();
        this.nodeCount = event.getNodeCount();
        this.instanceGroup = event.getInstanceGroup();
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Date getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Date eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getEventMessage() {
        return eventMessage;
    }

    public void setEventMessage(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
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

    public long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(long blueprintId) {
        this.blueprintId = blueprintId;
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
}
