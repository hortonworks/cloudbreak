package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;


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
}
