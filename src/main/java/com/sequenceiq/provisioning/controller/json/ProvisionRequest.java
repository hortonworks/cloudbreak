package com.sequenceiq.provisioning.controller.json;

import com.amazonaws.regions.Regions;
import com.sequenceiq.provisioning.domain.CloudPlatform;

public class ProvisionRequest {

    private String clusterName;
    private int clusterSize;
    private Regions region;
    private String keyName;
    private String accessKey;
    private String secretKey;
    private CloudPlatform type;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(int clusterSize) {
        this.clusterSize = clusterSize;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public CloudPlatform getType() {
        return type;
    }

    public void setType(CloudPlatform type) {
        this.type = type;
    }
}
