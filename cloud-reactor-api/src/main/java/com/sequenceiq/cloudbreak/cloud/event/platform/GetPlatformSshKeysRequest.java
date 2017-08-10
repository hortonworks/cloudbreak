package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

public class GetPlatformSshKeysRequest extends CloudPlatformRequest<GetPlatformSshKeysResult> {

    private String region;

    private String variant;

    private ExtendedCloudCredential extendedCloudCredential;

    private Map<String, String> filters = new HashMap<>();

    public GetPlatformSshKeysRequest(CloudCredential cloudCredential, ExtendedCloudCredential extendedCloudCredential, String variant, String region,
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

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }

    public String getRegion() {
        return region;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "GetPlatformSshKeysRequest{}";
    }
    //END GENERATED CODE
}
