package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@Component
public class ClusterRequestToGatewayConverter extends AbstractConversionServiceAwareConverter<ClusterRequest, Gateway> {

    @Inject
    private GatewayConvertUtil gatewayConvertUtil;

    @Override
    public Gateway convert(ClusterRequest cluster) {
        GatewayJson gatewayJson = cluster.getGateway();
        boolean legacyGatewayRequest = gatewayConvertUtil.isLegacyGatewayRequest(gatewayJson);
        if (legacyGatewayRequest && !gatewayJson.isEnableGateway()) {
            return null;
        }
        Gateway gateway = new Gateway();
        gatewayConvertUtil.setBasicProperties(gatewayJson, gateway);
        gatewayConvertUtil.setTopologies(gatewayJson, gateway);
        gatewayConvertUtil.setGatewayPathAndSsoProvider(cluster.getName(), gatewayJson, gateway);
        return gateway;
    }
}
