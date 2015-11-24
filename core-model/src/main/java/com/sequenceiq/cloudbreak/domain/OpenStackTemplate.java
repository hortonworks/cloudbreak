package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

@Entity
public class OpenStackTemplate extends Template implements ProvisionEntity {

    public OpenStackTemplate() {
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public String getVolumeTypeName() {
        return "HDD";
    }

}
