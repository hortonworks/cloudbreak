package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

@Component
public class SaltBootstrapFactory {

    public SaltBootstrap of(SaltStateService saltStateService, SaltConnector sc, Collection<SaltConnector> saltConnectors, List<GatewayConfig> allGatewayConfigs,
            Set<Node> targets, BootstrapParams params) {
        return new SaltBootstrap(saltStateService, sc, saltConnectors, allGatewayConfigs, targets, params);
    }
}
