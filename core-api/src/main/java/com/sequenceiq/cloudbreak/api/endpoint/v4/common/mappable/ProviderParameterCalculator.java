package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProviderParameterCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderParameterCalculator.class);

    public Mappable get(ProviderParametersBase source) {
        Mappable mappable = getMappable(source, source.getCloudPlatform());
        return mappable == null ? new Mappable() {
            @Override
            public Map<String, Object> asMap() {
                return null;
            }

            @Override
            public CloudPlatform getCloudPlatform() {
                return null;
            }
        } : mappable;
    }

    private Mappable getMappable(ProviderParametersBase source, CloudPlatform cloudPlatform) {
        Mappable mappable = null;
        switch (cloudPlatform) {
            case AWS:
                mappable = source.createAws();
                break;
            case GCP:
                mappable = source.createGcp();
                break;
            case MOCK:
                mappable = source.createMock();
                break;
            case YARN:
                mappable = source.createYarn();
                break;
            case AZURE:
                mappable = source.createAzure();
                break;
            case OPENSTACK:
                mappable = source.createOpenstack();
                break;
            case CUMULUS_YARN:
                mappable = source.createYarn();
                break;
        }
        if (mappable != null) {
            return mappable;
        }
        LOGGER.info("No mappable for cloudplatform [{}] and source [{}]", source.getCloudPlatform(), source.getClass());
        return null;
    }

    public void parse(Map<String, Object> parameters, ProviderParametersBase base) {
        if (parameters != null) {
            if (parameters.get("cloudPlatform") != null) {
                CloudPlatform cloudPlatform = CloudPlatform.valueOf(parameters.get("cloudPlatform").toString());
                Mappable mappable = getMappable(base, cloudPlatform);
                if (mappable != null) {
                    mappable.parse(parameters);
                }
            } else {
                LOGGER.info("Cannot determine cloudPlatform for {}", parameters);
            }
        }
    }

    private Map<String, Object> cleanNullValue(Map<String, Object> parameters) {
        Map<String, Object> ret = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (value != null) {
                ret.put(key, value);
            }
        });
        return ret;
    }
}
