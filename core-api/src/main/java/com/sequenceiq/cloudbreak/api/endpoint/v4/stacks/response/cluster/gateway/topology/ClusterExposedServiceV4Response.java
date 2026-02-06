package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterExposedServiceV4Response implements JsonEntity {

    private String serviceName;

    private String displayName;

    private String knoxService;

    private String serviceUrl;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean open;

    private SSOType mode;

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

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public SSOType getMode() {
        return mode;
    }

    public void setMode(SSOType mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "ClusterExposedServiceResponse{"
                + "serviceName='" + serviceName + '\''
                + ", displayName='" + displayName + '\''
                + ", knoxService='" + knoxService + '\''
                + ", serviceUrl='" + serviceUrl + '\''
                + ", open=" + open
                + ", mode=" + mode
                + '}';
    }
}
