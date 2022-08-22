package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class StateRunner extends BaseSaltJobRunner {
    //CHECKSTYLE:OFF
    protected final String state;
    //CHECKSTYLE:ON

    public StateRunner(SaltStateService saltStateService, Set<String> targetHostnames, Set<Node> allNode, String state) {
        super(saltStateService, targetHostnames, allNode);
        this.state = state;
    }

    @Override
    public String submit(SaltConnector saltConnector) throws SaltJobFailedException {
        HostList targets = new HostList(getTargetHostnames());
        return saltStateService().applyState(saltConnector, state, targets).getJid();
    }

    @Override
    public String toString() {
        return "StateRunner{" + super.toString() + ", state: " + this.state + "}'";
    }

    public String getState() {
        return state;
    }
}
