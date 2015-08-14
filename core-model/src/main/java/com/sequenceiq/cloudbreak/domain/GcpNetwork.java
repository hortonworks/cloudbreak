package com.sequenceiq.cloudbreak.domain;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;

@Entity
public class GcpNetwork extends Network {

    @Override
    public List<CloudPlatform> cloudPlatform() {
        return Arrays.asList(CloudPlatform.GCP);
    }
}
