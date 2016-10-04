package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class SyncGrainsRunner extends BaseSaltJobRunner {

    public SyncGrainsRunner(Set<String> target, Set<Node> allNode) {
        super(target, allNode);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        ApplyResponse grainsResult = SaltStates.syncGrains(saltConnector);
        Set<String> strings = collectMissingNodes(collectNodes(grainsResult));
        setTarget(strings);
        return strings.toString();
    }

    @Override
    public String toString() {
        return "SyncGrainsChecker{" + super.toString() + "}";
    }

}
