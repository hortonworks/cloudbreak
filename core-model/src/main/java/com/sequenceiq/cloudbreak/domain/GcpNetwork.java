package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

@Entity
public class GcpNetwork extends Network {

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }
}
