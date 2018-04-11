package com.sequenceiq.cloudbreak.templateprocessor.template.views;

import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.SSOType;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.json.Json;
import org.apache.commons.lang3.StringEscapeUtils;

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

    private final String tokenCert;

    public GatewayView(Gateway gateway) {
        enableGateway = gateway.getEnableGateway();
        gatewayType = gateway.getGatewayType();
        path = gateway.getPath();
        topologyName = gateway.getTopologyName();
        exposedServices = gateway.getExposedServices();
        ssoType = gateway.getSsoType();
        ssoProvider = gateway.getSsoProvider();
        signKey = gateway.getSignKey();
        signPub = gateway.getSignPub();
        signCert = gateway.getSignCert();
        tokenCert = gateway.getTokenCert();
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
        String cert = null;
        if (signCert != null) {
            cert = StringEscapeUtils.escapeJson(signCert.replaceAll("-----BEGIN CERTIFICATE-----|-----END CERTIFICATE-----", "").trim());
        }
        return cert;
    }

    public String getTokenCert() {
        return tokenCert;
    }
}
