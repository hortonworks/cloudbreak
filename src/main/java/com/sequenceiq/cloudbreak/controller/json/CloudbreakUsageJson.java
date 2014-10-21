package com.sequenceiq.cloudbreak.controller.json;

public class CloudbreakUsageJson implements JsonEntity {
    private String owner;

    private String account;

    private String blueprintName;
    private Long blueprintId;

    private String day;

    private String cloud;

    private String zone;

    private String machineType;

    private String runningHours;

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

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudbreakUsageJson{");
        sb.append("owner='").append(owner).append('\'');
        sb.append(", account='").append(account).append('\'');
        sb.append(", blueprintName='").append(blueprintName).append('\'');
        sb.append(", blueprintId=").append(blueprintId);
        sb.append(", day='").append(day).append('\'');
        sb.append(", cloud='").append(cloud).append('\'');
        sb.append(", zone='").append(zone).append('\'');
        sb.append(", machineType='").append(machineType).append('\'');
        sb.append(", runningHours='").append(runningHours).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
