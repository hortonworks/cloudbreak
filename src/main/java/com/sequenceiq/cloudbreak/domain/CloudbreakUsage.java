package com.sequenceiq.cloudbreak.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class CloudbreakUsage implements ProvisionEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String owner;

    private String account;

    private String blueprintName;
    private Long blueprintId;

    private Date day;

    private String cloud;

    private String zone;

    private String machineType;

    private String runningHours;

    private Long stackId;

    @Enumerated(EnumType.STRING)
    private Status stackStatus;

    private String stackName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public Status getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(Status stackStatus) {
        this.stackStatus = stackStatus;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudbreakUsage{");
        sb.append("id=").append(id);
        sb.append(", owner='").append(owner).append('\'');
        sb.append(", account='").append(account).append('\'');
        sb.append(", blueprintName='").append(blueprintName).append('\'');
        sb.append(", blueprintId=").append(blueprintId);
        sb.append(", day=").append(day);
        sb.append(", cloud='").append(cloud).append('\'');
        sb.append(", zone='").append(zone).append('\'');
        sb.append(", machineType='").append(machineType).append('\'');
        sb.append(", runningHours='").append(runningHours).append('\'');
        sb.append(", stackId='").append(stackId).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
