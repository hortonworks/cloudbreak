package com.sequenceiq.cloudbreak.api.endpoint.v4.knoxservices.responses;

import java.util.Collection;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;

@ApiModel
@NotNull
public class ExposedServiceV4Responses {

    private Collection<ExposedServiceV4Response> exposedServices;

    public Collection<ExposedServiceV4Response> getExposedServices() {
        return exposedServices;
    }

    public void setExposedServices(Collection<ExposedServiceV4Response> exposedServices) {
        this.exposedServices = exposedServices;
    }

    public static ExposedServiceV4Responses exposedServiceV4Responses(Collection<ExposedServiceV4Response> exposedServices) {
        ExposedServiceV4Responses responses = new ExposedServiceV4Responses();
        responses.setExposedServices(exposedServices);
        return responses;
    }
}
