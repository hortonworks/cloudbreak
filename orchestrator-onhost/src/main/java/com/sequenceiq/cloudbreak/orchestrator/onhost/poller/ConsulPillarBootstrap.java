package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;

public class ConsulPillarBootstrap implements OrchestratorBootstrap {

    private final OnHostClient client;
    private final String path;
    private final Map<String, Object> consulConfig;

    public ConsulPillarBootstrap(OnHostClient client, String path, Set<String> consulServers) {
        this.client = client;
        this.path = path;
        Map<String, Object> conf = new HashMap<>();
        Map<String, Object> conf2 = new HashMap();
        conf2.put("bootstrap_expect", consulServers.size());
        conf2.put("retry_join", consulServers);
        conf.put("consul", conf2);
        consulConfig = Collections.unmodifiableMap(conf);
    }

    @Override
    public Boolean call() throws Exception {
        if (!client.copySaltPillar(path, consulConfig)) {
            throw new CloudbreakOrchestratorFailedException("Failed to save salt pillar config");
        }
        return true;
    }
}
