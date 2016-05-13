package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

import java.util.Set;

public class ConsulChecker extends BaseSaltJobRunner {

    public ConsulChecker(Set<String> target) {
        super(target);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        return SaltStates.consul(saltConnector, new Compound(getTarget()));
    }

    @Override
    public String toString() {
        return "ConsulChecker{" + super.toString() + "}";
    }
}
