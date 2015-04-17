package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

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

}
