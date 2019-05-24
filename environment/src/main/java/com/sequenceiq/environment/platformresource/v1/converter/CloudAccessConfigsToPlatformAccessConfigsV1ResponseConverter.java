package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfig;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformAccessConfigsResponse;

@Component
public class CloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudAccessConfigs, PlatformAccessConfigsResponse> {

    @Override
    public PlatformAccessConfigsResponse convert(CloudAccessConfigs source) {
        PlatformAccessConfigsResponse platformAccessConfigsResponse = new PlatformAccessConfigsResponse();
        Set<AccessConfigResponse> result = new HashSet<>();
        for (CloudAccessConfig entry : source.getCloudAccessConfigs()) {
            AccessConfigResponse actual = new AccessConfigResponse(entry.getName(), entry.getId(), entry.getProperties());
            result.add(actual);
        }
        platformAccessConfigsResponse.setAccessConfigs(result);
        return platformAccessConfigsResponse;
    }
}
