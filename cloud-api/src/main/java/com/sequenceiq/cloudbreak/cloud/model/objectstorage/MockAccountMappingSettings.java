package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

public class MockAccountMappingSettings {

    private String region;

    private String adminGroupName;

    public MockAccountMappingSettings() {
    }

    public MockAccountMappingSettings(String region, String adminGroupName) {
        this.region = region;
        this.adminGroupName = adminGroupName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public void setAdminGroupName(String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }

}
