package com.sequenceiq.sdx.api.model;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ResourceUpdateRequest;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

public class SdxResourceUpdateRequest extends ResourceUpdateRequest {

    @ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE)
    public String getCrn() {
        return super.getCrn();
    }
}
