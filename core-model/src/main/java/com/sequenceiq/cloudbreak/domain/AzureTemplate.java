package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

//@Entity
public class AzureTemplate extends Template implements ProvisionEntity {

    public AzureTemplate() {
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    public String getVolumeTypeName() {
        return "HDD";
    }

}


