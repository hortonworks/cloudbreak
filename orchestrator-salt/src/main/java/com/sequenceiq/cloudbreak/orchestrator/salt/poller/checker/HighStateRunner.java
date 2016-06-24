package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class HighStateRunner extends BaseSaltJobRunner {

    public HighStateRunner(Set<String> target, Set<Node> allNode) {
        super(target, allNode);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        return SaltStates.highstate(saltConnector);
    }

    @Override
    public StateType stateType() {
        return StateType.HIGH;
    }

    @Override
    public String toString() {
        return "HighStateChecker{" + super.toString() + "}";
    }
}
