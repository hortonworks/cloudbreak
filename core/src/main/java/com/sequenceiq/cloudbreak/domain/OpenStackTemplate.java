package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

@Entity
public class OpenStackTemplate extends Template implements ProvisionEntity {

    private String instanceType;
    private String publicNetId;

    public OpenStackTemplate() {
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getPublicNetId() {
        return publicNetId;
    }

    public void setPublicNetId(String publicNetId) {
        this.publicNetId = publicNetId;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

}
