package com.sequenceiq.cloudbreak.common.mappable;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProviderParameterCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderParameterCalculator.class);

    public Mappable get(ProviderParametersBase source) {
        return getMappable(source, source.getCloudPlatform());
    }

    private Mappable getMappable(ProviderParametersBase source, CloudPlatform cloudPlatform) {
        return switch (cloudPlatform) {
            case AWS -> source.createAws();
            case GCP -> source.createGcp();
            case MOCK -> source.createMock();
            case YARN -> source.createYarn();
            case AZURE -> source.createAzure();
            default -> throw new BadRequestException(format("No mappable for cloudplatform [%s] and source [%s]", cloudPlatform, source.getClass()));
        };
    }

    public void parse(Map<String, Object> parameters, ProviderParametersBase base) {
        if (parameters != null) {
            if (parameters.get("cloudPlatform") != null) {
                CloudPlatform cloudPlatform = CloudPlatform.valueOf(parameters.get("cloudPlatform").toString());
                Map<String, Object> map = cleanNullValue(parameters);
                if (map.size() > 1) {
                    Mappable mappable = getMappable(base, cloudPlatform);
                    mappable.parse(map);
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
