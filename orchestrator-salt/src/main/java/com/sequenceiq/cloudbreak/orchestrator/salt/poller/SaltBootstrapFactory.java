package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.MinionUtil;

@Component
public class SaltBootstrapFactory {

    @Inject
    private SaltStateService saltStateService;

    @Inject
    private MinionUtil minionUtil;

    public SaltBootstrap of(SaltConnector sc, Collection<SaltConnector> saltConnectors, List<GatewayConfig> allGatewayConfigs,
            Set<Node> targets, BootstrapParams params) {
        return new SaltBootstrap(saltStateService, minionUtil, sc, saltConnectors, allGatewayConfigs, targets, params);
    }
}
