package com.sequenceiq.cloudbreak.converter.util;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class GatewayConvertUtil {

    @Inject
    private ConversionService conversionService;

    public void setTopologies(GatewayJson source, Gateway gateway) {
        if (isLegacyTopologyRequest(source)) {
            setLegacyTopology(gateway, source.getTopologyName(), source.getExposedServices());
        } else {
            setTopologyList(gateway, source.getTopologies());
        }
    }

    public boolean isLegacyTopologyRequest(GatewayJson source) {
        return StringUtils.isNotBlank(source.getTopologyName()) || !CollectionUtils.isEmpty(source.getExposedServices());
    }

    public void setGatewayPathAndSsoProvider(String clusterName, GatewayJson gatewayJson, Gateway gateway) {
        gateway.setPath(clusterName);
        if (gatewayJson.getPath() != null) {
            gateway.setPath(gatewayJson.getPath());
        }
        if (gateway.getSsoProvider() == null) {
            gateway.setSsoProvider('/' + gateway.getPath() + "/sso/api/v1/websso");
        }
    }

    public void setBasicProperties(GatewayJson source, Gateway gateway) {
        if (source.getGatewayType() != null) {
            gateway.setGatewayType(source.getGatewayType());
        }
        gateway.setSsoType(source.getSsoType() != null ? source.getSsoType() : SSOType.NONE);
        gateway.setTokenCert(source.getTokenCert());
        gateway.setKnoxMasterSecret(PasswordUtil.generatePassword());
    }

    public void generateSignKeys(Gateway gateway) {
        if (gateway != null) {
            if (gateway.getSignCert() == null) {
                KeyPair identityKey = PkiUtil.generateKeypair();
                KeyPair signKey = PkiUtil.generateKeypair();
                X509Certificate cert = PkiUtil.cert(identityKey, "signing", signKey);

                gateway.setSignKey(PkiUtil.convert(identityKey.getPrivate()));
                gateway.setSignPub(PkiUtil.convert(identityKey.getPublic()));
                gateway.setSignCert(PkiUtil.convert(cert));
            }
        }
    }

    public boolean isDisabledLegacyGateway(GatewayJson gatewayJson) {
        return gatewayJson.hasGatewayEnabled() && !gatewayJson.isEnableGateway();
    }

    private void setTopologyList(Gateway gateway, Collection<GatewayTopologyJson> topologies) {
        if (!CollectionUtils.isEmpty(topologies)) {
            Set<GatewayTopology> gatewayTopologies = topologies.stream()
                    .map(g -> conversionService.convert(g, GatewayTopology.class))
                    .collect(Collectors.toSet());
            gateway.setTopologies(gatewayTopologies);
            gatewayTopologies.forEach(g -> g.setGateway(gateway));
        }
    }

    private void setLegacyTopology(Gateway gateway, String topologyName, List<String> exposedServices) {
        if (StringUtils.isEmpty(topologyName)) {
            topologyName = "services";
        }
        GatewayTopology gatewayTopology = CollectionUtils.isEmpty(exposedServices)
                ? new GatewayTopology()
                : doLegacyConversion(topologyName, exposedServices);
        gatewayTopology.setTopologyName(topologyName);
        gatewayTopology.setGateway(gateway);
        gateway.setTopologies(Collections.singleton(gatewayTopology));
    }

    private GatewayTopology doLegacyConversion(String topologyName, List<String> exposedServices) {
        GatewayTopologyJson legacyTopology = new GatewayTopologyJson();
        legacyTopology.setTopologyName(topologyName);
        legacyTopology.setExposedServices(exposedServices);
        return conversionService.convert(legacyTopology, GatewayTopology.class);
    }
}
