package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template.views;

import org.apache.commons.lang3.StringEscapeUtils;

import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.SSOType;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.json.Json;

public class GatewayView {

    private final Boolean enableGateway;

    private final GatewayType gatewayType;

    private final String path;

    private final String topologyName;

    private final Json exposedServices;

    private final SSOType ssoType;

    private final String ssoProvider;

    private final String signKey;

    private final String signPub;

    private final String signCert;

    public GatewayView(Gateway gateway) {
        this.enableGateway = gateway.getEnableGateway();
        this.gatewayType = gateway.getGatewayType();
        this.path = gateway.getPath();
        this.topologyName = gateway.getTopologyName();
        this.exposedServices = gateway.getExposedServices();
        this.ssoType = gateway.getSsoType();
        this.ssoProvider = gateway.getSsoProvider();
        this.signKey = gateway.getSignKey();
        this.signPub = gateway.getSignPub();
        this.signCert = gateway.getSignCert();
    }

    public Boolean getEnableGateway() {
        return enableGateway;
    }

    public GatewayType getGatewayType() {
        return gatewayType;
    }

    public String getPath() {
        return path;
    }

    public String getTopologyName() {
        return topologyName;
    }

    public Json getExposedServices() {
        return exposedServices;
    }

    public SSOType getSsoType() {
        return ssoType;
    }

    public String getSsoProvider() {
        return ssoProvider;
    }

    public String getSignKey() {
        return signKey;
    }

    public String getSignPub() {
        return signPub;
    }

    public String getSignCert() {
        return signCert;
    }

    public String getEscSignKey() {
        return StringEscapeUtils.escapeJson(signKey);
    }

    public String getEscSignPub() {
        return StringEscapeUtils.escapeJson(signPub);
    }

    public String getEscSignCert() {
        return StringEscapeUtils.escapeJson(signCert);
    }



}
