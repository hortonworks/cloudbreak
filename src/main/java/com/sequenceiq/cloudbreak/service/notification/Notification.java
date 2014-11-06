package com.sequenceiq.cloudbreak.service.notification;

import java.util.Date;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public class Notification {

    private String eventType;
    private Date eventTimestamp;
    private String eventMessage;
    private String userName;
    private long userId;
    private String accountName;
    private long accountId;
    private String cloud;
    private String region;
    private String vmType;
    private String blueprintName;
    private long blueprintId;
    private Long stackId;

    public Notification() {
    }

    public Notification(CloudbreakEvent event) {
        this.eventType = event.getEventType();
        this.eventTimestamp = event.getEventTimestamp();
        this.eventMessage = event.getEventMessage();
        this.userName = event.getUserName();
        this.userId = event.getUserId();
        this.accountName = event.getAccountName();
        this.accountId = event.getAccountId();
        this.cloud = event.getCloud();
        this.region = event.getRegion();
        this.vmType = event.getVmType();
        this.blueprintName = event.getBlueprintName();
        this.blueprintId = event.getBlueprintId();
        this.stackId = event.getStackId();
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
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

    public String getVmType() {
        return vmType;
    }

    public void setVmType(String vmType) {
        this.vmType = vmType;
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
}
