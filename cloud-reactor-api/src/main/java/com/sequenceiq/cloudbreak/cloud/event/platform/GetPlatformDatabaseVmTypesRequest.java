package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.common.api.type.CdpResourceType;

public class GetPlatformDatabaseVmTypesRequest extends CloudPlatformRequest<GetPlatformDatabaseVmTypesResult> {

    private final String region;

    private final String variant;

    private final ExtendedCloudCredential extendedCloudCredential;

    private final CdpResourceType cdpResourceType;

    private final Map<String, String> filters;

    public GetPlatformDatabaseVmTypesRequest(CloudCredential cloudCredential, ExtendedCloudCredential extendedCloudCredential,
            String variant, String region, CdpResourceType cdpResourceType, Map<String, String> filters) {
        super(null, cloudCredential);
        this.extendedCloudCredential = extendedCloudCredential;
        this.variant = variant;
        this.region = region;
        this.filters = filters;
        this.cdpResourceType = cdpResourceType;
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

    public CdpResourceType getCdpResourceType() {
        return cdpResourceType;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    @Override
    public String toString() {
        return "GetPlatformDatabaseVmTypesRequest{" +
                "region='" + region + '\'' +
                ", variant='" + variant + '\'' +
                ", cdpResourceType=" + cdpResourceType +
                ", filters=" + filters +
                '}';
    }
}
