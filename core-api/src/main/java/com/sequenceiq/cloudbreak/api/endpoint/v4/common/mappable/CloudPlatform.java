package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import java.util.Map;

public enum CloudPlatform {
    AWS, GCP, AZURE, OPENSTACK, CUMULUS_YARN, YARN, MOCK;

    public void parse(Map<String, Object> parameters, ProviderParametersBase base) {
        switch (this) {
            case AWS:
                base.getAws().parse(parameters);
                break;
            case GCP:
                base.getGcp().parse(parameters);
                break;
            case CUMULUS_YARN:
                base.getYarn().parse(parameters);
                break;
            case OPENSTACK:
                base.getGcp().parse(parameters);
                break;
            case AZURE:
                base.getAzure().parse(parameters);
                break;
            case YARN:
                base.getYarn().parse(parameters);
                break;
            case MOCK:
                base.getMock().parse(parameters);
                break;
            default:
                throw new IllegalStateException("Unhandled CloudPlatform constant: " + name());
        }
    }
}
