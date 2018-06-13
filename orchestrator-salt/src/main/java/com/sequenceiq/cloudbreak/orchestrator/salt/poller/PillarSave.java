package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static java.util.Collections.singletonMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;

public class PillarSave implements OrchestratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(PillarSave.class);

    private final SaltConnector sc;

    private final Pillar pillar;

    private Set<String> targets;

    private final Set<String> originalTargets;

    public PillarSave(SaltConnector sc, Set<String> targets, Collection<Node> hosts) {
        this.sc = sc;
        Map<String, Map<String, Object>> fqdn = hosts
                .stream()
                .filter(node -> node.getHostname() != null)
                .collect(Collectors.toMap(Node::getPrivateIp, node -> discovery(node.getHostname(), node.getPublicIp())));
        pillar = new Pillar("/nodes/hosts.sls", singletonMap("hosts", fqdn), targets);
        this.targets = targets;
        originalTargets = targets;
    }

    public PillarSave(SaltConnector sc, Set<String> targets, Map<String, List<RecipeModel>> recipes) {
        this.sc = sc;
        Map<String, Map<String, List<String>>> scripts = new HashMap<>(recipes.size());
        for (Entry<String, List<RecipeModel>> entry : recipes.entrySet()) {
            List<String> preAmbariStart = entry.getValue().stream().
                    filter(h -> h.getRecipeType() == RecipeType.PRE_AMBARI_START).map(RecipeModel::getName).collect(Collectors.toList());
            List<String> preTermination = entry.getValue().stream().
                    filter(h -> h.getRecipeType() == RecipeType.PRE_TERMINATION).map(RecipeModel::getName).collect(Collectors.toList());
            List<String> postAmbariStart = entry.getValue().stream().
                filter(h -> h.getRecipeType() == RecipeType.POST_AMBARI_START).map(RecipeModel::getName).collect(Collectors.toList());
            List<String> postClusterInstall = entry.getValue().stream().
                    filter(h -> h.getRecipeType() == RecipeType.POST_CLUSTER_INSTALL).map(RecipeModel::getName).collect(Collectors.toList());
            Map<String, List<String>> prePostScripts = new HashMap<>();
            prePostScripts.put(RecipeExecutionPhase.PRE_AMBARI_START.value(), preAmbariStart);
            prePostScripts.put(RecipeExecutionPhase.PRE_TERMINATION.value(), preTermination);
            prePostScripts.put(RecipeExecutionPhase.POST_AMBARI_START.value(), postAmbariStart);
            prePostScripts.put(RecipeExecutionPhase.POST_CLUSTER_INSTALL.value(), postClusterInstall);
            scripts.put(entry.getKey(), prePostScripts);
        }
        pillar = new Pillar("/recipes/init.sls", singletonMap("recipes", scripts), targets);
        this.targets = targets;
        originalTargets = targets;
    }

    public PillarSave(SaltConnector sc, Set<String> targets, SaltPillarProperties pillarProperties) {
        this.sc = sc;
        pillar = new Pillar(pillarProperties.getPath(), pillarProperties.getProperties(), targets);
        this.targets = targets;
        originalTargets = targets;
    }

    private Map<String, Object> discovery(String hostname, String publicAddress) {
        Map<String, Object> map = new HashMap<>();
        map.put("fqdn", hostname);
        map.put("hostname", hostname.split("\\.")[0]);
        map.put("domain", hostname.replaceFirst(hostname.split("\\.")[0] + '.', ""));
        // Deprecated: this is just for backward compatibility, it is no longer in use
        map.put("custom_domain", true);
        map.put("public_address", StringUtils.isEmpty(publicAddress) ? Boolean.FALSE : Boolean.TRUE);
        return map;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Distribute pillar configs to: {}", targets);
        if (!targets.isEmpty()) {

            GenericResponses responses = sc.pillar(targets, pillar);

            Set<String> failedTargets = new HashSet<>();
            LOGGER.info("Salt pillar save responses: {}", responses);
            for (GenericResponse genericResponse : responses.getResponses()) {
                if (genericResponse.getStatusCode() != HttpStatus.OK.value()) {
                    LOGGER.info("Failed pillar save attempt to: {}, error: {}", genericResponse.getAddress(), genericResponse.getErrorText());
                    String address = genericResponse.getAddress().split(":")[0];
                    failedTargets.addAll(originalTargets.stream().filter(a -> a.equals(address)).collect(Collectors.toList()));
                }
            }
            targets = failedTargets;

            if (!targets.isEmpty()) {
                LOGGER.info("Missing nodes for pillar save: {}", targets);
                throw new CloudbreakOrchestratorFailedException("There are missing nodes for pillar save: " + targets);
            }
        }

        LOGGER.info("Pillar save has been completed on nodes: {}", originalTargets);
        return true;
    }
}
