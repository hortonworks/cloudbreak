package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Collections;
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
        response.setTopologies(getIfNotNull(source.getExposedServices(), this::topologies));
        response.setGatewayType(source.getGatewayType());
        response.setSsoProvider(source.getSsoProvider());
        response.setSsoType(source.getSsoType());
        response.setTokenCert(source.getTokenCert());
        return response;
    }

    public GatewayV1Request convert(GatewayV4Request source) {
        GatewayV1Request response = new GatewayV1Request();
        response.setPath(source.getPath());
        response.setExposedServices(getIfNotNull(source.getTopologies(), this::exposedService));
        response.setGatewayType(source.getGatewayType());
        response.setSsoProvider(source.getSsoProvider());
        response.setSsoType(source.getSsoType());
        response.setTokenCert(source.getTokenCert());
        return response;
    }

    private List<String> exposedService(List<GatewayTopologyV4Request> topologies) {
        return topologies.stream().flatMap(s -> s.getExposedServices().stream()).collect(Collectors.toList());
    }

    private List<GatewayTopologyV4Request> topologies(List<String> exposedServices) {
        GatewayTopologyV4Request topologyV4Request = new GatewayTopologyV4Request();
        topologyV4Request.setExposedServices(exposedServices);
        return Collections.singletonList(topologyV4Request);
    }
}
