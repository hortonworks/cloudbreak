package com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain;

public class SupportedVersion {

    private SupportedServices supportedServices;

    private String version;

    private String type;

    public SupportedServices getSupportedServices() {
        return supportedServices;
    }

    public void setSupportedServices(SupportedServices supportedServices) {
        this.supportedServices = supportedServices;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}