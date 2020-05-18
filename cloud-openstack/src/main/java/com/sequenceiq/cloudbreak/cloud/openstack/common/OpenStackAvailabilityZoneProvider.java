package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.List;

import org.openstack4j.api.OSClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;

@Service
public class OpenStackAvailabilityZoneProvider {

    @Cacheable(cacheNames = "cloudResourceAzCache", key = "{ #cloudCredential?.id, #regionFromOpenStack }")
    public List<AvailabilityZone> getAvailabilityZones(OpenStackClient openStackClient, OSClient<?> osClient,
            String regionFromOpenStack, CloudCredential cloudCredential) {
        return openStackClient.getZones(osClient, regionFromOpenStack);
    }

}
