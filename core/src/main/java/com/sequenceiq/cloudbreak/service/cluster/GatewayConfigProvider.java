package com.sequenceiq.cloudbreak.service.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Component
public class GatewayConfigProvider {

    @Inject
    private GatewayConfigService gatewayConfigService;

    public GatewayConfig getGatewayConfig(Stack stack) {
        Boolean enableKnox = stack.getCluster().getGateway() != null;
        GatewayConfig gatewayConfig = null;
        for (InstanceMetaData gateway : stack.getNotTerminatedGatewayInstanceMetadata()) {
            if (InstanceMetadataType.GATEWAY_PRIMARY.equals(gateway.getInstanceMetadataType())) {
                gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gateway, enableKnox);
            }
        }
        return gatewayConfig;
    }
}
