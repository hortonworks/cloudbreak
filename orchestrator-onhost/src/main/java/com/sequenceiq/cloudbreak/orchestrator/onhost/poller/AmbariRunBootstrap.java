package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;
import com.sequenceiq.cloudbreak.orchestrator.onhost.salt.SaltConnection;
import com.sequenceiq.cloudbreak.orchestrator.onhost.salt.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.onhost.salt.target.Compound;
import com.suse.salt.netapi.client.SaltClient;
import com.suse.salt.netapi.exception.SaltException;

public class AmbariRunBootstrap implements OrchestratorBootstrap {

    private final OnHostClient client;
    private final SaltClient saltClient;

    public AmbariRunBootstrap(OnHostClient client) throws SaltException {
        this.client = client;
        this.saltClient = new SaltConnection().get(client.getGatewayPublicIp());
    }

    @Override
    public Boolean call() throws Exception {
        SaltStates.ambariServer().callAsync(saltClient, new Compound("S@" + client.getGatewayPrivateIp()));
        SaltStates.ambariAgent().callAsync(saltClient, new Compound("* and not S@" + client.getGatewayPrivateIp()));
        return true;
    }
}
