package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Collection;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ExposedServiceV4Responses {

    private Collection<ExposedServiceV4Response> exposedServices;

    public Collection<ExposedServiceV4Response> getExposedServices() {
        return exposedServices;
    }

    public void setExposedServices(Collection<ExposedServiceV4Response> exposedServices) {
        this.exposedServices = exposedServices;
    }

    public static final ExposedServiceV4Responses exposedServiceV4Responses(Collection<ExposedServiceV4Response> exposedServices) {
        ExposedServiceV4Responses exposedServiceV4Responses = new ExposedServiceV4Responses();
        exposedServiceV4Responses.setExposedServices(exposedServices);
        return exposedServiceV4Responses;
    }
}
