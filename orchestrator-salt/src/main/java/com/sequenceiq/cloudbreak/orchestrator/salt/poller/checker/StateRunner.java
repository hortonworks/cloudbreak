package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

public class StateRunner extends BaseSaltJobRunner {
    //CHECKSTYLE:OFF
    protected final String state;
    //CHECKSTYLE:ON

    public StateRunner(SaltStateService saltStateService, Set<String> targetHostnames, String state) {
        super(saltStateService, targetHostnames, new HashSet<>());
        this.state = state;
    }

    @Override
    public String submit(SaltConnector saltConnector) throws SaltJobFailedException {
        HostList targets = new HostList(getTargetHostnames());
        return getJid(saltStateService().applyState(saltConnector, state, targets));
    }

    @Override
    public String toString() {
        return "StateRunner{" + super.toString() + ", state: " + this.state + "}'";
    }

    public String getState() {
        return state;
    }
}