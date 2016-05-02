package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;
import com.sequenceiq.cloudbreak.orchestrator.onhost.salt.SaltConnection;
import com.suse.salt.netapi.calls.modules.Test;
import com.suse.salt.netapi.client.SaltClient;
import com.suse.salt.netapi.datatypes.target.Glob;
import com.suse.salt.netapi.exception.SaltException;

import java.util.Map;
import java.util.Set;

public class SaltBootstrap implements OrchestratorBootstrap {

    private final OnHostClient client;
    private Set<String> missingTargets;

    public SaltBootstrap(OnHostClient client) throws SaltException {
        this.client = client;
        this.missingTargets = client.getTargets();

    }

    @Override
    public Boolean call() throws Exception {
        missingTargets = client.startSaltServiceOnTargetMachines(missingTargets);
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
