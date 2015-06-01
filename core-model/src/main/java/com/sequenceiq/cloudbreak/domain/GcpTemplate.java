package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
public class GcpTemplate extends Template implements ProvisionEntity {

    @Enumerated(EnumType.STRING)
    private GcpInstanceType gcpInstanceType;
    @Enumerated(EnumType.STRING)
    private GcpRawDiskType gcpRawDiskType = GcpRawDiskType.HDD;

    public GcpTemplate() {

    }

    public GcpInstanceType getGcpInstanceType() {
        return gcpInstanceType;
    }

    public void setGcpInstanceType(GcpInstanceType gcpInstanceType) {
        this.gcpInstanceType = gcpInstanceType;
    }

    public GcpRawDiskType getGcpRawDiskType() {
        return gcpRawDiskType;
    }

    public void setGcpRawDiskType(GcpRawDiskType gcpRawDiskType) {
        this.gcpRawDiskType = gcpRawDiskType;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }

}
