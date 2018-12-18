package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@Component
public class GatewayJsonToGatewayConverter extends AbstractConversionServiceAwareConverter<GatewayJson, Gateway> {

    @Inject
    private GatewayConvertUtil gatewayConvertUtil;

    @Override
    public Gateway convert(GatewayJson source) {
        Gateway gateway = new Gateway();
        gatewayConvertUtil.setBasicProperties(source, gateway);
        gatewayConvertUtil.setTopologies(source, gateway);
        return gateway;
    }
}
