package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@Component
public class GatewayJsonToGatewayConverter extends AbstractConversionServiceAwareConverter<GatewayJson, Gateway> {

    @Override
    public Gateway convert(GatewayJson source) {
        boolean legacyGatewayRequest = isLegacyGatewayRequest(source);
        if (legacyGatewayRequest && !source.isEnableGateway()) {
            return null;
        }
        Gateway gateway = new Gateway();
        setBasicProperties(source, gateway);
        setTopologies(source, gateway, legacyGatewayRequest);
        return gateway;
    }

    private boolean isLegacyGatewayRequest(GatewayJson source) {
        return StringUtils.isNotBlank(source.getTopologyName()) || !CollectionUtils.isEmpty(source.getExposedServices());
    }

    private void setBasicProperties(GatewayJson source, Gateway gateway) {
        if (source.getGatewayType() != null) {
            gateway.setGatewayType(source.getGatewayType());
        }
        gateway.setSsoType(source.getSsoType() != null ? source.getSsoType() : SSOType.NONE);
        gateway.setTokenCert(source.getTokenCert());
    }

    private void setTopologies(GatewayJson source, Gateway gateway, boolean legacyGatewayRequest) {
        if (legacyGatewayRequest) {
            setLegacyTopology(gateway, source.getTopologyName(), source.getExposedServices());
        } else {
            setTopologyList(gateway, source.getTopologies());
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
        return getConversionService().convert(legacyTopology, GatewayTopology.class);
    }

    private void setTopologyList(Gateway gateway, List<GatewayTopologyJson> topologies) {
        if (!CollectionUtils.isEmpty(topologies)) {
            Set<GatewayTopology> gatewayTopologies = topologies.stream()
                    .map(g -> getConversionService().convert(g, GatewayTopology.class))
                    .collect(Collectors.toSet());
            gateway.setTopologies(gatewayTopologies);
            gatewayTopologies.forEach(g -> g.setGateway(gateway));
        }
    }
}
