package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;


public class AmbariAgentAddRoleChecker extends BaseSaltJobRunner {

    public AmbariAgentAddRoleChecker(Target<String> target) {
        super(target);
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        return SaltStates.addRole(saltConnector, getTarget(), "ambari_agent").toString();
    }
}
