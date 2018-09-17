package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVmtypesResponse;

public class VmTypeEntity extends AbstractCloudbreakEntity<PlatformResourceRequestJson, PlatformVmtypesResponse, VmTypeEntity> {
    public static final String VMTYPE = "VMTYPE";

    VmTypeEntity(String newId) {
        super(newId);
        setRequest(new PlatformResourceRequestJson());
    }

    VmTypeEntity() {
        this(VMTYPE);
    }

    public VmTypeEntity withCredentialId(Long credentialId) {
        getRequest().setCredentialId(credentialId);
        return this;
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

    public VmTypeEntity withFilters(Map<String, String> filters) {
        getRequest().setFilters(filters);
        return this;
    }
}
