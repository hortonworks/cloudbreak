package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

public class GetPlatformSecurityGroupsRequest extends CloudPlatformRequest<GetPlatformSecurityGroupsResult> {

    private final String variant;

    private final String region;

    private final ExtendedCloudCredential extendedCloudCredential;

    private Map<String, String> filters = new HashMap<>();

    public GetPlatformSecurityGroupsRequest(CloudCredential cloudCredential, ExtendedCloudCredential extendedCloudCredential, String variant, String region,
            Map<String, String> filters) {
        super(null, cloudCredential);
        this.extendedCloudCredential = extendedCloudCredential;
        this.variant = variant;
        this.region = region;
        this.filters = filters;
    }

    public String getVariant() {
        return variant;
    }

    public String getRegion() {
        return region;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "GetPlatformSecurityGroupsRequest{}";
    }
    //END GENERATED CODE
}
