package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.AccessConfigJson;
import com.sequenceiq.cloudbreak.api.model.PlatformAccessConfigsResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfig;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class CloudAccessConfigsToPlatformAccessConfigsResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudAccessConfigs, PlatformAccessConfigsResponse> {

    @Override
    public PlatformAccessConfigsResponse convert(CloudAccessConfigs source) {
        PlatformAccessConfigsResponse platformAccessConfigsResponse = new PlatformAccessConfigsResponse();
        Set<AccessConfigJson> result = new HashSet<>();
        for (CloudAccessConfig entry : source.getCloudAccessConfigs()) {
            AccessConfigJson actual = new AccessConfigJson(entry.getName(), entry.getId(), entry.getProperties());
            result.add(actual);
        }
        platformAccessConfigsResponse.setAccessConfigs(result);
        return platformAccessConfigsResponse;
    }
}
