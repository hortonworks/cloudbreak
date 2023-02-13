package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformVmtypesV4Response;
import com.sequenceiq.it.cloudbreak.PlatformResourceParameters;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public class VmTypeTestDto extends AbstractCloudbreakTestDto<PlatformResourceParameters, PlatformVmtypesV4Response, VmTypeTestDto> {
    protected VmTypeTestDto(PlatformResourceParameters request, TestContext testContext) {
        super(request, testContext);
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
