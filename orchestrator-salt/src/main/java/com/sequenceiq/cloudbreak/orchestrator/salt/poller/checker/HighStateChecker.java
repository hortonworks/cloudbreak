package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

import java.util.Set;


public class HighStateChecker extends BaseSaltJobRunner {

    public HighStateChecker(Set<String> target) {
        super(target);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        return SaltStates.highstate(saltConnector).toString();
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
