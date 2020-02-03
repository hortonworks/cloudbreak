package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfig;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigResponseComparator;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformAccessConfigsResponse;

@Component
public class CloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudAccessConfigs, PlatformAccessConfigsResponse> {

    private static final byte MAX_AMOUNT_OF_ACC_CONFIGS_IN_CLOUD_ACC_CONFIGS = 50;

    @Override
    public PlatformAccessConfigsResponse convert(CloudAccessConfigs source) {
        PlatformAccessConfigsResponse platformAccessConfigsResponse = new PlatformAccessConfigsResponse();
        List<AccessConfigResponse> result = new ArrayList<>();
        for (CloudAccessConfig entry : source.getCloudAccessConfigs()) {
            for (byte i = 0; i < MAX_AMOUNT_OF_ACC_CONFIGS_IN_CLOUD_ACC_CONFIGS; i++) {
                AccessConfigResponse actual = new AccessConfigResponse(entry.getName() + i, entry.getId(), entry.getProperties());
                result.add(actual);
            }
        }
        if (result.size() % 2 == 0) {
            result.add(new AccessConfigResponse(result.get(0).getName() + new Date().getTime(), result.get(0).getId(), result.get(0).getProperties()));
        }
        result.sort(new AccessConfigResponseComparator());
        platformAccessConfigsResponse.setAccessConfigs(result);
        return platformAccessConfigsResponse;
    }

}
