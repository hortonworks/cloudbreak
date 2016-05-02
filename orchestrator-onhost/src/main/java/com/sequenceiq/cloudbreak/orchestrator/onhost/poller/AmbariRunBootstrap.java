package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.onhost.salt.SaltConnection;
import com.sequenceiq.cloudbreak.orchestrator.onhost.salt.SaltStates;
import com.suse.salt.netapi.client.SaltClient;
import com.suse.salt.netapi.datatypes.target.Glob;
import com.suse.salt.netapi.exception.SaltException;

public class AmbariRunBootstrap implements OrchestratorBootstrap {

    private final SaltClient saltClient;

    public AmbariRunBootstrap(String gatewayPublicIp) throws SaltException {
        this.saltClient = new SaltConnection().get(gatewayPublicIp);
    }

    @Override
    public Boolean call() throws Exception {
        //        SaltStates.ambariServer().callAsync(saltClient, new Compound("S@" + client.getGatewayPrivateIp()));
        //        SaltStates.ambariAgent().callAsync(saltClient, new Compound("* and not S@" + client.getGatewayPrivateIp()));
        SaltStates.highstate().callAsync(saltClient, Glob.ALL);
        return true;
    }
}
