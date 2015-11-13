package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.GcpInstanceType;

@Entity
public class GcpTemplate extends Template implements ProvisionEntity {

    @Enumerated(EnumType.STRING)
    private GcpInstanceType gcpInstanceType;
    private String gcpRawDiskType = "pd-standard";

    public GcpTemplate() {

    }

    public GcpInstanceType getGcpInstanceType() {
        return gcpInstanceType;
    }

    public void setGcpInstanceType(GcpInstanceType gcpInstanceType) {
        this.gcpInstanceType = gcpInstanceType;
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
    public String getInstanceTypeName() {
        return getGcpInstanceType().getValue();
    }

    @Override
    public String getVolumeTypeName() {
        return getGcpRawDiskType();
    }

}
