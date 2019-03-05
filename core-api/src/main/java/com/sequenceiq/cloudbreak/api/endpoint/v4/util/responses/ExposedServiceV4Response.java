package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ExposedServiceV4Response {

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

    public static Collection<ExposedServiceV4Response> fromExposedServices(Collection<ExposedService> exposedServices) {
        List<ExposedServiceV4Response> exposedServiceV4ResponseList = new ArrayList<>();
        for (ExposedService service : exposedServices) {
            ExposedServiceV4Response exposedServiceV4Response = new ExposedServiceV4Response();
            exposedServiceV4Response.displayName = service.getPortName();
            exposedServiceV4Response.serviceName = service.getServiceName();
            exposedServiceV4Response.knoxService = service.getKnoxService();
            exposedServiceV4Response.knoxUrl = service.getKnoxUrl();
            exposedServiceV4ResponseList.add(exposedServiceV4Response);
        }
        return exposedServiceV4ResponseList;
    }
}
