package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

public class GatewayToGatewayV4ResponseConverter extends AbstractConversionServiceAwareConverter<Gateway, GatewayV4Response> {

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public GatewayV4Response convert(Gateway source) {
        GatewayV4Response response = new GatewayV4Response();
        response.setGatewayType(source.getGatewayType());
        response.setPath(source.getPath());
        response.setSsoProvider(source.getSsoProvider());
        response.setSsoType(source.getSsoType());
        response.setTokenCert(source.getTokenCert());
        response.setTopologies(converterUtil.convertAll(source.getTopologies(), GatewayTopologyV4Response.class));
        return response;
    }
}
