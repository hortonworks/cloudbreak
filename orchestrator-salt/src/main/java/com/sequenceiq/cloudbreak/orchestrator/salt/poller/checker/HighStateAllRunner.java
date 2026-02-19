package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class HighStateAllRunner extends BaseSaltJobRunner {

    public HighStateAllRunner(SaltStateService saltStateService, Set<String> target, Set<Node> allNode) {
        super(saltStateService, target, allNode);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        return getJid(saltStateService().highstate(saltConnector));
    }

    @Override
    public StateType stateType() {
        return StateType.HIGH;
    }

    @Override
    public String toString() {
        return "HighStateAllRunner{" + super.toString() + '}';
    }
}