package com.sequenceiq.cloudbreak.domain.cloudstorage;

import java.util.ArrayList;
import java.util.List;

public class CloudStorage {

    private String s3GuardDynamoTableName;

    private List<StorageLocation> locations = new ArrayList<>();

    private List<CloudIdentity> cloudIdentities = new ArrayList<>();

    private AccountMapping accountMapping;

    public String getS3GuardDynamoTableName() {
        return s3GuardDynamoTableName;
    }

    public void setS3GuardDynamoTableName(String s3GuardDynamoTableName) {
        this.s3GuardDynamoTableName = s3GuardDynamoTableName;
    }

    public List<StorageLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<StorageLocation> locations) {
        if (locations == null) {
            locations = new ArrayList<>();
        }
        this.locations = locations;
    }

    public List<CloudIdentity> getCloudIdentities() {
        return cloudIdentities;
    }

    public void setCloudIdentities(List<CloudIdentity> cloudIdentities) {
        if (cloudIdentities == null) {
            cloudIdentities = new ArrayList<>();
        }
        this.cloudIdentities = cloudIdentities;
    }

    public AccountMapping getAccountMapping() {
        return accountMapping;
    }

    public void setAccountMapping(AccountMapping accountMapping) {
        this.accountMapping = accountMapping;
    }

}
