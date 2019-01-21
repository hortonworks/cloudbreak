package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import java.util.Map;

public enum CloudPlatform {
    AWS, GCP, AZURE, OPENSTACK, CUMULUS_YARN, YARN, MOCK;

    public <T> T to(Map<String, Object> parameters, ProviderParametersBase base) {
        switch (this) {
            case AWS:
                return base.getAws().toClass(parameters);
            case GCP:
                return base.getGcp().toClass(parameters);
            case CUMULUS_YARN:
                return base.getYarn().toClass(parameters);
            case OPENSTACK:
                return base.getGcp().toClass(parameters);
            case AZURE:
                return base.getAzure().toClass(parameters);
            case YARN:
                return base.getYarn().toClass(parameters);
            case MOCK:
                return base.getMock().toClass(parameters);
            default:
                return null;
        }
    }
}
