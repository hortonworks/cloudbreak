package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class MineUpdateRunner extends BaseSaltJobRunner {

    public MineUpdateRunner(SaltStateService saltStateService, Set<String> target, Set<Node> allNode) {
        super(saltStateService, target, allNode);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        ApplyResponse grainsResult = saltStateService().updateMine(saltConnector);
        Set<String> strings = collectMissingHostnames(collectSucceededNodes(grainsResult));
        setTargetHostnames(strings);
        return strings.toString();
    }

    @Override
    public String toString() {
        return "MineUpdateRunner{" + super.toString() + '}';
    }

}
