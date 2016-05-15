package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;


public class SimpleAddRoleChecker extends BaseSaltJobRunner {

    private String type;

    public SimpleAddRoleChecker(Set<String> target, String type) {
        super(target);
        this.type = type;
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        ApplyResponse response = SaltStates.addRole(saltConnector, new Compound(getTarget()), type);
        Set<String> missingIps = collectMissingNodes(saltConnector, collectNodes(response));
        setTarget(missingIps);
        return missingIps.toString();
    }

    @Override
    public String toString() {
        return "SimpleAddRoleChecker{"
                + "type='" + type + '\'' + super.toString()
                + '}';
    }
}
