package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@Component
public class StackV2RequestToGatewayConverter extends AbstractConversionServiceAwareConverter<StackV2Request, Gateway> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV2RequestToGatewayConverter.class);

    @Inject
    private GatewayConvertUtil convertUtil;

    @Override
    public Gateway convert(StackV2Request source) {
        Gateway gateway = new Gateway();
        GatewayJson gatewayJson = source.getCluster().getAmbari().getGateway();
        convertUtil.setBasicProperties(gatewayJson, gateway);
        convertUtil.setTopologies(gatewayJson, gateway);
        convertUtil.setGatewayPathAndSsoProvider(source.getGeneral().getName(), gatewayJson, gateway);
        convertUtil.generateSignKeys(gateway);
        return gateway;
    }

}
