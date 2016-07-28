package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
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

    public PillarSave(SaltConnector sc, Set<Node> hosts, boolean useCustomDomain) {
        this.sc = sc;
        Map<String, Map<String, Object>> fqdn = hosts
                .stream()
                .collect(Collectors.toMap(Node::getPrivateIp, node -> discovery(node.getHostname(), node.getPublicIp(), useCustomDomain)));
        this.pillar = new Pillar("/nodes/hosts.sls", singletonMap("hosts", fqdn));
    }

    public PillarSave(SaltConnector sc, Map<String, List<RecipeModel>> recipes) {
        this.sc = sc;
        Map<String, Map<String, List<String>>> scripts = new HashMap<>();
        for (String hostGroup : recipes.keySet()) {
            List<String> pre = recipes.get(hostGroup).stream().filter(h -> h.getPreInstall() != null).map(RecipeModel::getName).collect(Collectors.toList());
            List<String> post = recipes.get(hostGroup).stream().filter(h -> h.getPostInstall() != null).map(RecipeModel::getName).collect(Collectors.toList());
            Map<String, List<String>> prePostScripts = new HashMap<>();
            prePostScripts.put("pre", pre);
            prePostScripts.put("post", post);
            scripts.put(hostGroup, prePostScripts);
        }
        this.pillar = new Pillar("/recipes/init.sls", singletonMap("recipes", scripts));
    }

    public PillarSave(SaltConnector sc, SaltPillarProperties pillarProperties) {
        this.sc = sc;
        this.pillar = new Pillar(pillarProperties.getPath(), pillarProperties.getProperties());
    }

    private Map<String, Object> discovery(String hostname, String publicAddress, boolean useCustomDomain) {
        Map<String, Object> map = new HashMap<>();
        map.put("fqdn", hostname);
        map.put("hostname", hostname.split("\\.")[0]);
        map.put("custom_domain", useCustomDomain);
        map.put("public_address", StringUtils.isEmpty(publicAddress) ? Boolean.FALSE : Boolean.TRUE);
        return map;
    }

    @Override
    public Boolean call() throws Exception {
        GenericResponse resp = sc.pillar(pillar);
        resp.assertError();
        return true;
    }
}
