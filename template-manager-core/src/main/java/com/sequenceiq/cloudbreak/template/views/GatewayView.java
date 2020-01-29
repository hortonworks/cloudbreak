package com.sequenceiq.cloudbreak.template.views;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

public class GatewayView {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayView.class);

    private final GatewayType gatewayType;

    private final String path;

    private final SSOType ssoType;

    private final boolean ssoConfigured;

    private final String ssoProvider;

    private final String signKey;

    private final String signPub;

    private final String signCert;

    private final String tokenCert;

    private final Map<String, Set<String>> gatewayTopologies;

    private final String masterSecret;

    public GatewayView(@Nonnull Gateway gateway, String signKey, Set<String> fullServiceList) {
        gatewayType = gateway.getGatewayType();
        path = gateway.getPath();
        gatewayTopologies = CollectionUtils.isEmpty(gateway.getTopologies())
                ? emptyMap()
                : gateway.getTopologies().stream()
                .collect(Collectors.toMap(GatewayTopology::getTopologyName,
                        s -> convertToFullExposedServices(s.getExposedServices(), fullServiceList)));
        ssoType = gateway.getSsoType();
        ssoConfigured = SSOType.SSO_PROVIDER.equals(gateway.getSsoType());
        ssoProvider = gateway.getSsoProvider();
        signPub = gateway.getSignPub();
        signCert = gateway.getSignCert();
        tokenCert = gateway.getTokenCert();
        this.masterSecret = gateway.getKnoxMasterSecret();
        this.signKey = signKey;
    }

    private Set<String> convertToFullExposedServices(Json json, Set<String> fullServiceList) {
        try {
            ExposedServices exposedServices = json.get(ExposedServices.class);
            if (exposedServices == null || exposedServices.getServices().isEmpty()) {
                return new HashSet<>();
            }
            if (exposedServices.getServices().contains("ALL")) {
                return fullServiceList;
            }
            return new HashSet<>(exposedServices.getServices());
        } catch (IOException e) {
            LOGGER.info("Cannot deserialize the exposed services: json: {}", json.getValue(), e);
        }
        try {
            return Set.of(json.get(String[].class));
        } catch (IOException e) {
            LOGGER.info("Cannot deserialize the set of services: {}", json.getValue(), e);
        }
        return emptySet();
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

    public Set<String> getExposedServices() {
        return gatewayTopologies.isEmpty() ? null : getFirstTopology().getValue();
    }

    private Entry<String, Set<String>> getFirstTopology() {
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

    public Map<String, Set<String>> getGatewayTopologies() {
        return gatewayTopologies;
    }

    public String getMasterSecret() {
        return masterSecret;
    }
}
