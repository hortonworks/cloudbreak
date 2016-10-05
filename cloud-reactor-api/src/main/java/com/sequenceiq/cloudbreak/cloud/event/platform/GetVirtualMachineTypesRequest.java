package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class GetVirtualMachineTypesRequest extends CloudPlatformRequest<GetVirtualMachineTypesResult> {
    private Boolean extended;

    public GetVirtualMachineTypesRequest(Boolean extended) {
        super(null, null);
        this.extended = extended;
    }

    public Boolean getExtended() {
        return extended;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "GetVirtualMachineTypesRequest{}";
    }
    //END GENERATED CODE
}
