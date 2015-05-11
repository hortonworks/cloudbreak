package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

@Entity
public class OpenStackNetwork extends Network {

    private String publicNetId;

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
