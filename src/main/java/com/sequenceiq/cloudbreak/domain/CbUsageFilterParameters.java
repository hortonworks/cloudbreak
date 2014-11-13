package com.sequenceiq.cloudbreak.domain;

public class CbUsageFilterParameters {
    private String account;
    private String owner;
    private Long since;
    private String cloud;
    private String region;
    private String vmType;
    private Long instanceHours;
    private Long bpId;
    private String bpName;

    public CbUsageFilterParameters(String account, String owner, Long since, String cloud, String region,
            String vmType, Long instanceHours, Long bpId, String bpName) {
        this.account = account;
        this.owner = owner;
        this.since = since;
        this.cloud = cloud;
        this.region = region;
        this.vmType = vmType;
        this.instanceHours = instanceHours;
        this.bpId = bpId;
        this.bpName = bpName;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Long getSince() {
        return since;
    }

    public void setSince(Long since) {
        this.since = since;
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

    public Long getInstanceHours() {
        return instanceHours;
    }

    public void setInstanceHours(Long instanceHours) {
        this.instanceHours = instanceHours;
    }

    public Long getBpId() {
        return bpId;
    }

    public void setBpId(Long bpId) {
        this.bpId = bpId;
    }

    public String getBpName() {
        return bpName;
    }

    public void setBpName(String bpName) {
        this.bpName = bpName;
    }
}
