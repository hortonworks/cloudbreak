package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.filters.PlatformResourceV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformVmtypesV4Response;

public class VmTypeEntity extends AbstractCloudbreakEntity<PlatformResourceV4Filter, PlatformVmtypesV4Response, VmTypeEntity> {
    public static final String VMTYPE = "VMTYPE";

    VmTypeEntity(String newId) {
        super(newId);
        setRequest(new PlatformResourceV4Filter());
    }

    VmTypeEntity() {
        this(VMTYPE);
    }

    public VmTypeEntity withCredentialName(String credentialName) {
        getRequest().setCredentialName(credentialName);
        return this;
    }

    public VmTypeEntity withRegion(String regionName) {
        getRequest().setRegion(regionName);
        return this;
    }

    public VmTypeEntity withPlatform(String platformVariant) {
        getRequest().setPlatformVariant(platformVariant);
        return this;
    }

    public VmTypeEntity withAvailabilityZone(String availabilityZone) {
        getRequest().setAvailabilityZone(availabilityZone);
        return this;
    }
}
