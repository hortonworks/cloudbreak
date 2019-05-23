package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNullF;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.gateway.GatewayV1Request;

@Component
public class GatewayV1ToGatewayV4Converter {

    public GatewayV4Request convert(GatewayV1Request source) {
        GatewayV4Request response = new GatewayV4Request();
        response.setPath(source.getPath());
        response.setTopologies(ifNotNullF(source.getExposedServices(), this::topologies));
        response.setGatewayType(source.getGatewayType());
        response.setSsoProvider(source.getSsoProvider());
        response.setSsoType(source.getSsoType());
        response.setTokenCert(source.getTokenCert());
        return response;
    }

    private List<GatewayTopologyV4Request> topologies(List<String> exposedServices) {
        return exposedServices.stream().map(e -> {
            GatewayTopologyV4Request gatewayTopologyV4Request = new GatewayTopologyV4Request();
            gatewayTopologyV4Request.setTopologyName("topology-name");
            gatewayTopologyV4Request.setExposedServices(exposedServices);
            return gatewayTopologyV4Request;
        }).collect(Collectors.toList());
    }
}
