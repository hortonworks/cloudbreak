package com.sequenceiq.environment.platformresource.converter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfig;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.platformresource.model.AccessConfigV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformAccessConfigsV1Response;

@Component
public class CloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudAccessConfigs, PlatformAccessConfigsV1Response> {

    @Override
    public PlatformAccessConfigsV1Response convert(CloudAccessConfigs source) {
        PlatformAccessConfigsV1Response platformAccessConfigsV1Response = new PlatformAccessConfigsV1Response();
        Set<AccessConfigV1Response> result = new HashSet<>();
        for (CloudAccessConfig entry : source.getCloudAccessConfigs()) {
            AccessConfigV1Response actual = new AccessConfigV1Response(entry.getName(), entry.getId(), entry.getProperties());
            result.add(actual);
        }
        platformAccessConfigsV1Response.setAccessConfigs(result);
        return platformAccessConfigsV1Response;
    }
}
