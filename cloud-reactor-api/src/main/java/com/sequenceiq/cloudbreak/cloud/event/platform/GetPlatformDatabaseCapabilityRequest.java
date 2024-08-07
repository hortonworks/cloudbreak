package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

public class GetPlatformDatabaseCapabilityRequest extends CloudPlatformRequest<GetPlatformDatabaseCapabilityResult> {

    private final String variant;

    private final String region;

    private final ExtendedCloudCredential extendedCloudCredential;

    private final Map<String, String> filters;

    public GetPlatformDatabaseCapabilityRequest(CloudCredential cloudCredential,
            ExtendedCloudCredential extendedCloudCredential, String variant, String region, Map<String, String> filters) {
        super(null, cloudCredential);
        this.extendedCloudCredential = extendedCloudCredential;
        this.variant = variant;
        this.region = region;
        this.filters = filters;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }

    public String getVariant() {
        return variant;
    }

    public String getRegion() {
        return region;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    @Override
    public String toString() {
        return "GetPlatformDatabaseCapabilityRequest{}";
    }

}
