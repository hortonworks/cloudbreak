package com.sequenceiq.cloudbreak.api.endpoint.v4.responses;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class ServiceTypeV4Response {

    @Schema
    private List<String> serviceTypes;

    public ServiceTypeV4Response() {
    }

    public ServiceTypeV4Response(List<String> serviceTypes) {
        this.serviceTypes = serviceTypes;
    }

    public List<String> getServiceTypes() {
        return serviceTypes;
    }

    public void setServiceTypes(List<String> servicetypes) {
        this.serviceTypes = servicetypes;
    }

    @Override
    public String toString() {
        return "ServiceTypeV4Response{" +
                "serviceTypes=" + serviceTypes +
                '}';
    }
}
