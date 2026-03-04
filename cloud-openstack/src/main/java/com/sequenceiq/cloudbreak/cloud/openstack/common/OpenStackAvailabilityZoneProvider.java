package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.List;

import org.openstack4j.api.OSClient;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClient;

@Service
public class OpenStackAvailabilityZoneProvider {

    public List<AvailabilityZone> getAvailabilityZones(OpenStackClient openStackClient, OSClient<?> osClient,
            String regionFromOpenStack) {
        return openStackClient.getZones(osClient, regionFromOpenStack);
    }

}
