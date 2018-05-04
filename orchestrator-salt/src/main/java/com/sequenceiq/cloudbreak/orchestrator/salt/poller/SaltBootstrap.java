package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Cloud;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAuth;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltMaster;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class SaltBootstrap implements OrchestratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltBootstrap.class);

    private final SaltConnector sc;

    private final List<GatewayConfig> allGatewayConfigs;

    private final Set<Node> originalTargets;

    private Set<Node> targets;

    private String domain;

    private BootstrapParams params;

    public SaltBootstrap(SaltConnector sc, List<GatewayConfig> allGatewayConfigs, Set<Node> targets, String domain, BootstrapParams params) {
        this.sc = sc;
        this.allGatewayConfigs = allGatewayConfigs;
        this.originalTargets = Collections.unmodifiableSet(targets);
        this.targets = targets;
        this.domain = domain;
        this.params = params;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Bootstrapping of nodes [{}/{}]", originalTargets.size() - targets.size(), originalTargets.size());
        if (!targets.isEmpty()) {
            LOGGER.info("Missing targets for SaltBootstrap: {}", targets);

            SaltAction saltAction = createBootstrap();
            GenericResponses responses = sc.action(saltAction);

            Set<Node> failedTargets = new HashSet<>();

            LOGGER.info("SaltBootstrap responses: {}", responses);
            for (GenericResponse genericResponse : responses.getResponses()) {
                if (genericResponse.getStatusCode() != HttpStatus.OK.value()) {
                    LOGGER.info("Failed to distributed salt run to: " + genericResponse.getAddress());
                    String address = genericResponse.getAddress().split(":")[0];
                    failedTargets.addAll(originalTargets.stream().filter(a -> a.getPrivateIp().equals(address)).collect(Collectors.toList()));
                }
            }
            targets = failedTargets;

            if (!targets.isEmpty()) {
                LOGGER.info("Missing nodes to run saltbootstrap: {}", targets);
                throw new CloudbreakOrchestratorFailedException("There are missing nodes from saltbootstrap: " + targets);
            }
        }

        Map<String, String> networkResult = SaltStates.networkInterfaceIP(sc, Glob.ALL).getResultGroupByIP();
        originalTargets.forEach(node -> {
            if (!networkResult.containsKey(node.getPrivateIp())) {
                LOGGER.info("Salt-minion is not responding on host: {}, yet", node);
                targets.add(node);
            }
        });
        if (!targets.isEmpty()) {
            throw new CloudbreakOrchestratorFailedException("There are missing nodes from salt network response: " + targets);
        }
        LOGGER.info("Bootstrapping of nodes completed: {}", originalTargets.size());
        return true;
    }

    private SaltAction createBootstrap() {
        SaltAction saltAction = new SaltAction(SaltActionType.RUN);
        if (params.getCloud() != null) {
            saltAction.setCloud(new Cloud(params.getCloud()));
        }
        SaltAuth auth = new SaltAuth();
        auth.setPassword(sc.getSaltPassword());
        List<String> targetIps = targets.stream().map(Node::getPrivateIp).collect(Collectors.toList());
        for (GatewayConfig gatewayConfig : allGatewayConfigs) {
            String gatewayAddress = gatewayConfig.getPrivateAddress();
            if (targetIps.contains(gatewayAddress)) {
                SaltMaster master = new SaltMaster();
                master.setAddress(gatewayAddress);
                master.setAuth(auth);
                master.setDomain(domain);
                saltAction.addMaster(master);
                // set due to compatibility reasons
                saltAction.setMaster(master);
                saltAction.setServer(gatewayAddress);
                Node saltMaster = targets.stream().filter(n -> n.getPrivateIp().equals(gatewayAddress)).findFirst().get();
                saltAction.addMinion(createMinion(saltMaster));
            }
        }
        for (Node minion : targets.stream().filter(node -> !getGatewayPrivateIps().contains(node.getPrivateIp())).collect(Collectors.toList())) {
            saltAction.addMinion(createMinion(minion));
        }
        return saltAction;
    }

    private Minion createMinion(Node node) {
        Minion minion = new Minion();
        minion.setAddress(node.getPrivateIp());
        minion.setRoles(Collections.emptyList());
        minion.setHostGroup(node.getHostGroup());
        minion.setDomain(domain);
        minion.setServers(getGatewayPrivateIps());
        // set due to compatibility reasons
        minion.setServer(getGatewayPrivateIps().get(0));
        return minion;
    }

    private List<String> getGatewayPrivateIps() {
        return allGatewayConfigs.stream().map(GatewayConfig::getPrivateAddress).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SaltBootstrap{");
        sb.append("sc=").append(sc);
        sb.append(", allGatewayConfigs=").append(allGatewayConfigs);
        sb.append(", originalTargets=").append(originalTargets);
        sb.append(", targets=").append(targets);
        sb.append(", domain='").append(domain).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
