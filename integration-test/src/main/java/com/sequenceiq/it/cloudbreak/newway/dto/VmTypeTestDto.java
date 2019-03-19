package com.sequenceiq.it.cloudbreak.newway.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformVmtypesV4Response;
import com.sequenceiq.it.cloudbreak.newway.PlatformResourceParameters;

public class VmTypeTestDto extends AbstractCloudbreakTestDto<PlatformResourceParameters, PlatformVmtypesV4Response, VmTypeTestDto> {
    public static final String VMTYPE = "VMTYPE";

    VmTypeTestDto(String newId) {
        super(newId);
        setRequest(new PlatformResourceParameters());
    }

    VmTypeTestDto() {
        this(VMTYPE);
    }

    public VmTypeTestDto withCredentialName(String credentialName) {
        getRequest().setCredentialName(credentialName);
        return this;
    }

    public VmTypeTestDto withRegion(String regionName) {
        getRequest().setRegion(regionName);
        return this;
    }

    public VmTypeTestDto withPlatform(String platformVariant) {
        getRequest().setPlatformVariant(platformVariant);
        return this;
    }

    public VmTypeTestDto withAvailabilityZone(String availabilityZone) {
        getRequest().setAvailabilityZone(availabilityZone);
        return this;
    }
}
