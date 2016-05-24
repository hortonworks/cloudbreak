package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;

public class PillarSave implements OrchestratorBootstrap {

    private final SaltConnector sc;
    private final Pillar pillar;

    public PillarSave(SaltConnector sc, String gateway) {
        this.sc = sc;
        this.pillar = new Pillar("/ambari/server.sls", singletonMap("ambari", singletonMap("server", gateway)));
    }

    public PillarSave(SaltConnector sc, Set<Node> hosts) {
        this.sc = sc;
        Map<String, Map<String, String>> fqdn = hosts
                .stream()
                .collect(Collectors.toMap(Node::getPrivateIp, node -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("fqdn", node.getHostname());
                    map.put("hostname", node.getHostname().split("\\.")[0]);
                    return map;
                }));
        this.pillar = new Pillar("/nodes/hosts.sls", singletonMap("hosts", fqdn));
    }

    public PillarSave(SaltConnector sc, SaltPillarProperties pillarProperties) {
        this.sc = sc;
        this.pillar = new Pillar(pillarProperties.getPath(), pillarProperties.getProperties());
    }

    @Override
    public Boolean call() throws Exception {
        GenericResponse resp = sc.pillar(pillar);
        resp.assertError();
        return true;
    }
}
