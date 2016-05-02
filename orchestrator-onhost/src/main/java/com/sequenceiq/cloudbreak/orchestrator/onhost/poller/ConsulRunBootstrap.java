package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;
import com.sequenceiq.cloudbreak.orchestrator.onhost.salt.SaltConnection;
import com.sequenceiq.cloudbreak.orchestrator.onhost.salt.SaltStates;
import com.suse.salt.netapi.client.SaltClient;
import com.suse.salt.netapi.datatypes.target.Glob;
import com.suse.salt.netapi.exception.SaltException;

public class ConsulRunBootstrap implements OrchestratorBootstrap {

    private final SaltClient saltClient;

    public ConsulRunBootstrap(OnHostClient client) throws SaltException {
        this.saltClient = new SaltConnection().get(client.getGatewayPublicIp());
    }

    @Override
    public Boolean call() throws Exception {
        SaltStates.consul().callAsync(saltClient, Glob.ALL);
        return true;
    }
}
