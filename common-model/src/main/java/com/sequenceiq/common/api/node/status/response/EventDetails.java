package com.sequenceiq.common.api.node.status.response;

public class EventDetails {

    private String meteredResourceCrn;

    private String meteredResourceName;

    private String serviceType;

    public String getMeteredResourceCrn() {
        return meteredResourceCrn;
    }

    public void setMeteredResourceCrn(String meteredResourceCrn) {
        this.meteredResourceCrn = meteredResourceCrn;
    }

    public String getMeteredResourceName() {
        return meteredResourceName;
    }

    public void setMeteredResourceName(String meteredResourceName) {
        this.meteredResourceName = meteredResourceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String toString() {
        return "EventDetails{" +
                "meteredresourcecrn='" + meteredResourceCrn + '\'' +
                ", meteredresourcename='" + meteredResourceName + '\'' +
                ", servicetype='" + serviceType + '\'' +
                '}';
    }
}
