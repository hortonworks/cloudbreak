package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class SyncAllRunner extends BaseSaltJobRunner {

    public SyncAllRunner(Set<String> targetHostnames, Set<Node> allNode) {
        super(targetHostnames, allNode);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        ApplyResponse grainsResult = SaltStates.syncAll(saltConnector);
        Set<String> strings = collectMissingHostnames(collectSucceededNodes(grainsResult));
        setTargetHostnames(strings);
        return grainsResult.getJid();
    }

    @Override
    public String toString() {
        return "SyncGrainsChecker{" + super.toString() + '}';
    }

}
