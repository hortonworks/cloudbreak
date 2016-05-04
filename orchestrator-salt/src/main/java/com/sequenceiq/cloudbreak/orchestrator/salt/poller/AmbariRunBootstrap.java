package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;


public class AmbariRunBootstrap implements OrchestratorBootstrap {

    private final SaltConnector saltConnector;

    public AmbariRunBootstrap(SaltConnector saltConnector) {
        this.saltConnector = saltConnector;
    }

    @Override
    public Boolean call() throws Exception {
        //        SaltStates.ambariServer().callAsync(saltClient, new Compound("S@" + client.getGatewayPrivateIp()));
        //        SaltStates.ambariAgent().callAsync(saltClient, new Compound("* and not S@" + client.getGatewayPrivateIp()));
        SaltStates.highstate(saltConnector, Glob.ALL);
        return true;
    }
}
