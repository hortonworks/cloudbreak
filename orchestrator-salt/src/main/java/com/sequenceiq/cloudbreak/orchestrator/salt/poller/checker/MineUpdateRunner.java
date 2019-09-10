package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class MineUpdateRunner extends BaseSaltJobRunner {

    public MineUpdateRunner(Set<String> targetHostnames, Set<Node> allNode) {
        super(targetHostnames, allNode);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        ApplyResponse grainsResult = SaltStates.updateMine(saltConnector);
        Set<String> missingHostnames = collectMissingHostnames(collectSucceededNodes(grainsResult));
        setTargetHostnames(missingHostnames);
        return missingHostnames.toString();
    }

    @Override
    public String toString() {
        return "MineUpdateRunner{" + super.toString() + '}';
    }

}
