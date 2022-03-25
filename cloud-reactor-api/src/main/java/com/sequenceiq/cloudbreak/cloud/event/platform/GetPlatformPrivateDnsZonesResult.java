package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;

public class GetPlatformPrivateDnsZonesResult extends CloudPlatformResult {

    private CloudPrivateDnsZones privateDnsZones;

    public GetPlatformPrivateDnsZonesResult(Long resourceId, CloudPrivateDnsZones privateDnsZones) {
        super(resourceId);
        this.privateDnsZones = privateDnsZones;
    }

    public GetPlatformPrivateDnsZonesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudPrivateDnsZones getPrivateDnsZones() {
        return privateDnsZones;
    }

}