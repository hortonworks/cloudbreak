package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import static java.util.Objects.isNull;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ProviderParameterCalculator {

    public Mappable get(ProviderParametersBase source) {
        if (isNull(source)) {
            return Mappable.EMPTY;
        }
        switch (source.getCloudPlatform()) {
            case AWS:
                return source.getAws();
            case GCP:
                return source.getGcp();
            case MOCK:
                return source.getMock();
            case YARN:
                return source.getYarn();
            case AZURE:
                return source.getAzure();
            case OPENSTACK:
                return source.getOpenstack();
            case CUMULUS_YARN:
                return source.getYarn();
            default:
                return Mappable.EMPTY;
        }
    }

    public void to(Map<String, Object> parameters, ProviderParametersBase base) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(parameters.get("cloudPlatform").toString());
        cloudPlatform.parse(parameters, base);
    }
}
