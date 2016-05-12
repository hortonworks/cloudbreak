package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class ConsulChecker extends BaseSaltJobRunner {

    public ConsulChecker(Set<String> target) {
        super(target);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        return SaltStates.consul(saltConnector, new Compound(getTarget()));
    }

}
