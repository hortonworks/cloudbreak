package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class GetVirtualMachineTypesRequest extends CloudPlatformRequest<GetVirtualMachineTypesResult> {

    private final Boolean extended;

    private final String type;

    public GetVirtualMachineTypesRequest(String type, Boolean extended) {
        super(null, null);
        this.extended = extended;
        this.type = type;
    }

    public Boolean getExtended() {
        return extended;
    }

    public String getType() {
        return type;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "GetVirtualMachineTypesRequest{}";
    }
    //END GENERATED CODE
}
