package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AccessConfigJson;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformAccessConfigsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfig;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class CloudAccessConfigsToPlatformAccessConfigsV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudAccessConfigs, PlatformAccessConfigsV4Response> {

    @Override
    public PlatformAccessConfigsV4Response convert(CloudAccessConfigs source) {
        PlatformAccessConfigsV4Response platformAccessConfigsV4Response = new PlatformAccessConfigsV4Response();
        Set<AccessConfigJson> result = new HashSet<>();
        for (CloudAccessConfig entry : source.getCloudAccessConfigs()) {
            AccessConfigJson actual = new AccessConfigJson(entry.getName(), entry.getId(), entry.getProperties());
            result.add(actual);
        }
        platformAccessConfigsV4Response.setAccessConfigs(result);
        return platformAccessConfigsV4Response;
    }
}
