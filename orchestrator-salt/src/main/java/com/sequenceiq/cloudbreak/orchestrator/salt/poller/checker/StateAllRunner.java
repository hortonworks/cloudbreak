package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class StateAllRunner extends BaseSaltJobRunner {

    private final String state;

    public StateAllRunner(SaltStateService saltStateService, Set<String> targetHostnames, Set<Node> allNode, String state) {
        super(saltStateService, targetHostnames, allNode);
        this.state = state;
    }

    @Override
    public String submit(SaltConnector saltConnector) throws SaltJobFailedException {
        return getJid(saltStateService().applyStateAll(saltConnector, state));
    }

    @Override
    public String toString() {
        return "StateAllRunner{" + super.toString() + ", state: " + this.state + "}'";
    }
}