package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;

@Component
public class GatewayV1ToGatewayV4Converter {

    @Value("${distrox.gateway.topology.name}")
    private String topologyName;

    @Value("${distrox.gateway.ssotype}")
    private SSOType ssoType;

    public GatewayV4Request convert(List<String> exposedServices) {
        GatewayV4Request gatewayV4Request = new GatewayV4Request();
        gatewayV4Request.setTopologies(getIfNotNull(exposedServices, this::topologies));
        gatewayV4Request.setSsoType(ssoType);
        return gatewayV4Request;
    }

    public List<String> exposedService(GatewayV4Request gateway) {
        return gateway.getTopologies().stream().flatMap(s -> s.getExposedServices().stream()).collect(Collectors.toList());
    }

    private List<GatewayTopologyV4Request> topologies(List<String> exposedServices) {
        GatewayTopologyV4Request topologyV4Request = new GatewayTopologyV4Request();
        topologyV4Request.setExposedServices(exposedServices);
        topologyV4Request.setTopologyName(topologyName);
        return Collections.singletonList(topologyV4Request);
    }
}
