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
    private GccZone gccZone;
    @Enumerated(EnumType.STRING)
    private GccInstanceType gccInstanceType;
    private Boolean moreContainerOnOneHost = Boolean.FALSE;
    private Integer containerCount = 0;
    @Enumerated(EnumType.STRING)
    private GccRawDiskType gccRawDiskType = GccRawDiskType.HDD;

    public GccTemplate() {

    }

    public GccZone getGccZone() {
        return gccZone;
    }

    public void setGccZone(GccZone gccZone) {
        this.gccZone = gccZone;
    }

    public GccInstanceType getGccInstanceType() {
        return gccInstanceType;
    }

    public void setGccInstanceType(GccInstanceType gccInstanceType) {
        this.gccInstanceType = gccInstanceType;
    }

    public Integer getContainerCount() {
        return containerCount;
    }

    public void setContainerCount(Integer containerCount) {
        this.containerCount = containerCount;
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

    @Override
    public Integer getMultiplier() {
        return containerCount == 0 ? 1 : containerCount;
    }
}
