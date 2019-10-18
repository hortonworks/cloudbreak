package com.sequenceiq.cloudbreak.template.views;

public class ClusterExposedServiceView {

    private String serviceName;

    private String displayName;

    private String knoxService;

    private String serviceUrl;

    public ClusterExposedServiceView(String serviceName, String displayName, String knoxService, String serviceUrl) {
        this.serviceName = serviceName;
        this.displayName = displayName;
        this.knoxService = knoxService;
        this.serviceUrl = serviceUrl;
    }

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

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @Override
    public String toString() {
        return "ClusterExposedServiceResponse{"
                + "serviceName='" + serviceName + '\''
                + ", displayName='" + displayName + '\''
                + ", knoxService='" + knoxService + '\''
                + ", serviceUrl='" + serviceUrl + '\''
                + '}';
    }
}
