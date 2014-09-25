package com.sequenceiq.cloudbreak.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class CloudbreakUsage implements ProvisionEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String userName;
    private Long userId;

    private String accountName;
    private Long accountId;

    private String blueprintName;
    private Long blueprintId;

    private Date day;

    private String cloud;

    private String zone;

    private String machineType;

    private String runningHours;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public String getRunningHours() {
        return runningHours;
    }

    public void setRunningHours(String runningHours) {
        this.runningHours = runningHours;
    }

    public Date getDay() {
        return day;
    }

    public void setDay(Date day) {
        this.day = day;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudbreakUsage{");
        sb.append("id=").append(id);
        sb.append(", userName='").append(userName).append('\'');
        sb.append(", userId=").append(userId);
        sb.append(", accountName='").append(accountName).append('\'');
        sb.append(", accountId=").append(accountId);
        sb.append(", blueprintName='").append(blueprintName).append('\'');
        sb.append(", blueprintId=").append(blueprintId);
        sb.append(", day=").append(day);
        sb.append(", cloud='").append(cloud).append('\'');
        sb.append(", zone='").append(zone).append('\'');
        sb.append(", machineType='").append(machineType).append('\'');
        sb.append(", runningHours='").append(runningHours).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
