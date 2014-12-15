package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccInstanceType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccRawDiskType;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain.GccZone;

@Entity
public class GccTemplate extends Template implements ProvisionEntity {

    @Enumerated(EnumType.STRING)
    private GccInstanceType gccInstanceType;
    @Enumerated(EnumType.STRING)
    private GccRawDiskType gccRawDiskType = GccRawDiskType.HDD;

    public GccTemplate() {

    }

    public GccInstanceType getGccInstanceType() {
        return gccInstanceType;
    }

    public void setGccInstanceType(GccInstanceType gccInstanceType) {
        this.gccInstanceType = gccInstanceType;
    }

    public GccRawDiskType getGccRawDiskType() {
        return gccRawDiskType;
    }

    public void setGccRawDiskType(GccRawDiskType gccRawDiskType) {
        this.gccRawDiskType = gccRawDiskType;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCC;
    }

}
