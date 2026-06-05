package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

public class StackResourceUpdateRequest extends ResourceUpdateRequest {

    public StackResourceUpdateRequest() {
    }

    public StackResourceUpdateRequest(ResourceUpdateRequest source) {
        setCrn(source.getCrn());
        setResourceId(source.getResourceId());
        setDiskSyncMode(source.getDiskSyncMode());
    }

    @ValidCrn(resource = { CrnResourceDescriptor.VM_DATALAKE, CrnResourceDescriptor.DATAHUB })
    public String getCrn() {
        return super.getCrn();
    }
}
