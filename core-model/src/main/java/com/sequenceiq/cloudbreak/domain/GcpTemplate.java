package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

@Entity
public class GcpTemplate extends Template implements ProvisionEntity {

    private String gcpRawDiskType = "pd-standard";

    public GcpTemplate() {

    }

    public String getGcpRawDiskType() {
        return gcpRawDiskType;
    }

    public void setGcpRawDiskType(String gcpRawDiskType) {
        this.gcpRawDiskType = gcpRawDiskType;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public String getVolumeTypeName() {
        return getGcpRawDiskType();
    }

}
