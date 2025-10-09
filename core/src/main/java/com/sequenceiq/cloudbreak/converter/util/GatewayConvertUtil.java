package com.sequenceiq.cloudbreak.converter.util;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToGatewayTopologyConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class GatewayConvertUtil {

    @Inject
    private GatewayTopologyV4RequestToGatewayTopologyConverter gatewayTopologyV4RequestToGatewayTopologyConverter;

    public void setTopologies(GatewayV4Request source, Gateway gateway) {
        if (!CollectionUtils.isEmpty(source.getTopologies())) {
            Set<GatewayTopology> gatewayTopologies = source.getTopologies().stream()
                    .map(g -> gatewayTopologyV4RequestToGatewayTopologyConverter.convert(g))
                    .collect(Collectors.toSet());
            gateway.setTopologies(gatewayTopologies);
            gatewayTopologies.forEach(g -> g.setGateway(gateway));
        }
    }

    public void setGatewayPathAndSsoProvider(GatewayV4Request source, Gateway gateway) {
        if (source.getPath() != null) {
            gateway.setPath(source.getPath());
        }
        if (gateway.getSsoProvider() == null) {
            gateway.setSsoProvider('/' + gateway.getPath() + "/sso/api/v1/websso");
        }
    }

    public void setBasicProperties(GatewayV4Request source, Gateway gateway) {
        if (source.getGatewayType() != null) {
            gateway.setGatewayType(source.getGatewayType());
        }
        gateway.setSsoType(source.getSsoType() != null ? source.getSsoType() : SSOType.NONE);
        gateway.setTokenCert(source.getTokenCert());
        gateway.setKnoxMasterSecret(PasswordUtil.generateCmAndPostgresConformPassword());
    }
}
