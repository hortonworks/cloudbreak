package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.requests.PlatformResourceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformVmtypesV4Response;

public class VmTypeEntity extends AbstractCloudbreakEntity<PlatformResourceV4Request, PlatformVmtypesV4Response, VmTypeEntity> {
    public static final String VMTYPE = "VMTYPE";

    VmTypeEntity(String newId) {
        super(newId);
        setRequest(new PlatformResourceV4Request());
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
