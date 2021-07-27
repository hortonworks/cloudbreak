package com.sequenceiq.cloudbreak.cloud.model;

public class CDPServicePolicyVerificationResponse {

    public static final Integer NOT_FOUND = 404;

    public static final Integer NOT_IMPLEMENTED = 501;

    public static final Integer SERVICE_UNAVAILABLE = 503;

    private String serviceName;

    private String serviceStatus;

    private Integer statusCode;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return "CDPServicePolicyVerificationResult{" +
                "serviceName='" + serviceName + '\'' +
                ", serviceStatus='" + serviceStatus + '\'' +
                ", statusCode='" + statusCode + '\'' +
                '}';
    }
}

