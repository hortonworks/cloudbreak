package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class SyncAllRunner extends BaseSaltJobRunner {

    public SyncAllRunner(SaltStateService saltStateService, Set<String> targetHostnames, Set<Node> allNode) {
        super(saltStateService, targetHostnames, allNode);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        ApplyResponse grainsResult = saltStateService().syncAll(saltConnector);
        Set<String> strings = collectMissingHostnames(collectSucceededNodes(grainsResult));
        setTargetHostnames(strings);
        return getJid(grainsResult);
    }

    @Override
    public String toString() {
        return "SyncGrainsChecker{" + super.toString() + '}';
    }

}