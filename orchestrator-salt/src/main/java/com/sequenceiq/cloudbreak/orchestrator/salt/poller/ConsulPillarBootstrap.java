package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltBootResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConsulPillarBootstrap implements OrchestratorBootstrap {

    private final SaltConnector sc;
    private final Pillar pillar;

    public ConsulPillarBootstrap(SaltConnector sc, Set<String> consulServers) {
        this.sc = sc;
        Map<String, Object> conf = new HashMap<>();
        Map<String, Object> consul = new HashMap();
        consul.put("bootstrap_expect", consulServers.size());
        consul.put("retry_join", consulServers);
        conf.put("consul", consul);
        pillar = new Pillar("/consul/init.sls", conf);
    }

    @Override
    public Boolean call() throws Exception {
        SaltBootResponse resp = sc.pillar(pillar);
        resp.assertError();
        return true;
    }
}
