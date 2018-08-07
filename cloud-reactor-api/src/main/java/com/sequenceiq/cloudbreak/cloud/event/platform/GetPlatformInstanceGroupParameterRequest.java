package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;

public class GetPlatformInstanceGroupParameterRequest extends CloudPlatformRequest<GetPlatformInstanceGroupParameterResult> {

    private final Set<InstanceGroupParameterRequest> instanceGroupParameterRequest;

    private final ExtendedCloudCredential extendedCloudCredential;

    private final String variant;

    public GetPlatformInstanceGroupParameterRequest(CloudCredential cloudCredential, ExtendedCloudCredential extendedCloudCredential,
            Set<InstanceGroupParameterRequest> instanceGroupParameterRequest, String variant) {
        super(null, cloudCredential);
        this.extendedCloudCredential = extendedCloudCredential;
        this.instanceGroupParameterRequest = instanceGroupParameterRequest;
        this.variant = variant;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }

    public String getVariant() {
        return variant;
    }

    public Set<InstanceGroupParameterRequest> getInstanceGroupParameterRequest() {
        return instanceGroupParameterRequest;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "GetPlatformInstanceGroupParameterRequest{}";
    }
    //END GENERATED CODE
}
