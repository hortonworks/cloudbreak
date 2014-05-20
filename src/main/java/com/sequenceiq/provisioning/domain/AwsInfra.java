package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;

@Entity
public class AwsInfra extends Infra implements ProvisionEntity {

    private String name;

    private String region;

    private String keyName;

    public AwsInfra() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }
}
