package com.sequenceiq.cloudbreak.domain;

import java.util.Arrays;
import java.util.List;

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
    public List<CloudPlatform> cloudPlatform() {
        return Arrays.asList(CloudPlatform.OPENSTACK);
    }
}
