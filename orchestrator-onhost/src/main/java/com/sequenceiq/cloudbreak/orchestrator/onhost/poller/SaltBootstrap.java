package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;
import com.sequenceiq.cloudbreak.orchestrator.onhost.salt.SaltConnection;
import com.suse.salt.netapi.calls.modules.Test;
import com.suse.salt.netapi.client.SaltClient;
import com.suse.salt.netapi.datatypes.target.Glob;

public class SaltBootstrap implements OrchestratorBootstrap {

    private final OnHostClient client;
    private Set<String> missingTargets;
    private final Set<String> consulServers;

    public SaltBootstrap(OnHostClient client, Set<String> consulServers) {
        this.client = client;
        this.missingTargets = client.getTargets();
        this.consulServers = consulServers;
    }

    @Override
    public Boolean call() throws Exception {
        missingTargets = client.startSaltServiceOnTargetMachines(missingTargets, consulServers);
        if (!missingTargets.isEmpty()) {
            throw new CloudbreakOrchestratorFailedException("There are missing nodes from salt: " + missingTargets);
        }

        SaltClient saltClient = new SaltConnection().get(client.getGatewayPublicIp());
        Map<String, Boolean> results = Test.ping().callSync(saltClient, Glob.ALL);
        if (client.getTargets().size() > results.size()) {
            throw new CloudbreakOrchestratorFailedException("There are missing nodes from salt: " + missingTargets);
        }
        return true;
    }
}
