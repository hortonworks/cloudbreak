package com.sequenceiq.cloudbreak.template.views;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

public class GatewayView {

    private final GatewayType gatewayType;

    private final String path;

    private final SSOType ssoType;

    private final boolean ssoConfigured;

    private final String ssoProvider;

    private final String signKey;

    private final String signPub;

    private final String signCert;

    private final String tokenCert;

    private final Map<String, Json> gatewayTopologies;

    public GatewayView(@Nonnull Gateway gateway, String signKey) {
        gatewayType = gateway.getGatewayType();
        path = gateway.getPath();
        gatewayTopologies = CollectionUtils.isEmpty(gateway.getTopologies()) ? Collections.emptyMap() : gateway.getTopologies().stream()
                .collect(Collectors.toMap(GatewayTopology::getTopologyName, GatewayTopology::getExposedServices));
        ssoType = gateway.getSsoType();
        ssoConfigured = SSOType.SSO_PROVIDER.equals(gateway.getSsoType());
        ssoProvider = gateway.getSsoProvider();
        signPub = gateway.getSignPub();
        signCert = gateway.getSignCert();
        tokenCert = gateway.getTokenCert();
        this.signKey = signKey;
    }

    public GatewayType getGatewayType() {
        return gatewayType;
    }

    public String getPath() {
        return path;
    }

    public String getTopologyName() {
        return gatewayTopologies.isEmpty() ? null : getFirstTopology().getKey();
    }

    public Json getExposedServices() {
        return gatewayTopologies.isEmpty() ? null : getFirstTopology().getValue();
    }

    private Entry<String, Json> getFirstTopology() {
        return gatewayTopologies.entrySet().iterator().next();
    }

    public SSOType getSsoType() {
        return ssoType;
    }

    public String getSsoProvider() {
        return ssoProvider;
    }

    public boolean isSsoConfigured() {
        return ssoConfigured;
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

    public String getSignCertWithoutHeader() {
        String cert = null;
        if (signCert != null) {
            cert = signCert.replaceAll("-----BEGIN CERTIFICATE-----|-----END CERTIFICATE-----", "").trim();
        }
        return cert;
    }

    public String getTokenCert() {
        return tokenCert;
    }

    public Map<String, Json> getGatewayTopologies() {
        return gatewayTopologies;
    }
}
