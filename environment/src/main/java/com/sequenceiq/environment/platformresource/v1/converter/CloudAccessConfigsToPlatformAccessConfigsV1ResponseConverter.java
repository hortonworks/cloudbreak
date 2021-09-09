package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfig;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigResponseComparator;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformAccessConfigsResponse;

@Component
public class CloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter {

    public PlatformAccessConfigsResponse convert(CloudAccessConfigs source) {
        PlatformAccessConfigsResponse platformAccessConfigsResponse = new PlatformAccessConfigsResponse();
        List<AccessConfigResponse> result = new ArrayList<>();
        for (CloudAccessConfig entry : source.getCloudAccessConfigs()) {
            AccessConfigResponse actual = new AccessConfigResponse(entry.getName(), entry.getId(), entry.getProperties());
            result.add(actual);
        }
        result.sort(new AccessConfigResponseComparator());
        platformAccessConfigsResponse.setAccessConfigs(result);
        return platformAccessConfigsResponse;
    }

}
