package com.sequenceiq.cloudbreak.api.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExposedServiceResponse {

    private String serviceName;

    private String displayName;

    private String knoxService;

    private String knoxUrl;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getKnoxService() {
        return knoxService;
    }

    public void setKnoxService(String knoxService) {
        this.knoxService = knoxService;
    }

    public String getKnoxUrl() {
        return knoxUrl;
    }

    public void setKnoxUrl(String knoxUrl) {
        this.knoxUrl = knoxUrl;
    }

    public static Collection<ExposedServiceResponse> fromExposedServices(Collection<ExposedService> exposedServices) {
        List<ExposedServiceResponse> exposedServiceResponseList = new ArrayList<>();
        for (ExposedService service : exposedServices) {
            ExposedServiceResponse exposedServiceResponse = new ExposedServiceResponse();
            exposedServiceResponse.displayName = service.getPortName();
            exposedServiceResponse.serviceName = service.getServiceName();
            exposedServiceResponse.knoxService = service.getKnoxService();
            exposedServiceResponse.knoxUrl = service.getKnoxUrl();
            exposedServiceResponseList.add(exposedServiceResponse);
        }
        return exposedServiceResponseList;
    }
}
