package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

@Entity
public class OpenStackTemplate extends Template implements ProvisionEntity {

    private String instanceType;

    public OpenStackTemplate() {
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public String getInstanceTypeName() {
        return getInstanceType();
    }

    @Override
    public String getVolumeTypeName() {
        return "HDD";
    }

}
