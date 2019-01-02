package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import static java.lang.String.format;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ProviderParameterCalculator {

    public Mappable get(ProviderParametersBase source) {
        Mappable mappable;
        switch (source.getCloudPlatform()) {
            case AWS:
                mappable = source.getAws();
                break;
            case GCP:
                mappable = source.getGcp();
                break;
            case MOCK:
                mappable = source.getMock();
                break;
            case YARN:
                mappable = source.getYarn();
                break;
            case AZURE:
                mappable = source.getAzure();
                break;
            case OPENSTACK:
                mappable = source.getOpenstack();
                break;
            case CUMULUS_YARN:
                mappable = source.getYarn();
                break;
            default:
                throw new IllegalArgumentException(format("No mappable for cloudplatform [%s] and source [%s]", source.getCloudPlatform(), source.getClass()));
        }
        return mappable == null ? () -> null : mappable;
    }

    public void to(Map<String, Object> parameters, ProviderParametersBase base) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(parameters.get("cloudPlatform").toString());
        cloudPlatform.parse(parameters, base);
    }
}
