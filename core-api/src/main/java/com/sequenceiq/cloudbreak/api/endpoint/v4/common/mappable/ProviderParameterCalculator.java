package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ProviderParameterCalculator {

    public Mappable get(ProviderParametersBase source) {
        if (source.getCloudPlatform() == null) {
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
        }
        return Mappable.EMPTY;
    }

    public void to(Map<String, Object> parameters, ProviderParametersBase base) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(parameters.get("cloudPlatform").toString());
        cloudPlatform.to(parameters, base);
    }
}
