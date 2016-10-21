package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAuth;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltMaster;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class SaltBootstrap implements OrchestratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltBootstrap.class);

    private final SaltConnector sc;
    private final GatewayConfig gatewayConfig;
    private final Set<Node> originalTargets;
    private Set<Node> targets;
    private String domain;

    public SaltBootstrap(SaltConnector sc, GatewayConfig gatewayConfig, Set<Node> targets, String domain) {
        this.sc = sc;
        this.gatewayConfig = gatewayConfig;
        this.originalTargets = Collections.unmodifiableSet(targets);
        this.targets = targets;
        this.domain = domain;
    }

    @Override
    public Boolean call() throws Exception {
        if (!targets.isEmpty()) {
            LOGGER.info("Missing targets for SaltBootstrap: {}", targets);

            SaltAction saltAction = createBootstrap();
            GenericResponses responses = sc.action(saltAction);

            Set<Node> failedTargets = new HashSet<>();

            LOGGER.info("Salt run response: {}", responses);
            for (GenericResponse genericResponse : responses.getResponses()) {
                if (genericResponse.getStatusCode() != HttpStatus.OK.value()) {
                    LOGGER.info("Successfully distributed salt run to: " + genericResponse.getAddress());
                    String address = genericResponse.getAddress().split(":")[0];
                    failedTargets.addAll(originalTargets.stream().filter(a -> a.getPrivateIp().equals(address)).collect(Collectors.toList()));
                }
            }
            targets = failedTargets;

            if (!targets.isEmpty()) {
                LOGGER.info("Missing nodes to run salt: %s", targets);
                throw new CloudbreakOrchestratorFailedException("There are missing nodes from salt: " + targets);
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
            throw new CloudbreakOrchestratorFailedException("There are missing nodes from salt: " + targets);
        }
        return true;
    }

    private SaltAction createBootstrap() {
        SaltAction saltAction = new SaltAction(SaltActionType.RUN);

        if (targets.stream().map(Node::getPrivateIp).collect(Collectors.toList()).contains(getGatewayPrivateIp())) {
            SaltAuth auth = new SaltAuth();
            auth.setPassword(sc.getPassword());
            SaltMaster master = new SaltMaster();
            master.setAddress(getGatewayPrivateIp());
            master.setAuth(auth);
            master.setDomain(domain);
            saltAction.setMaster(master);
            //set due to compatibility reason
            saltAction.setServer(getGatewayPrivateIp());
            Node saltMaster = targets.stream().filter(n -> n.getPrivateIp().equals(getGatewayPrivateIp())).findFirst().get();
            saltAction.addMinion(createMinion(saltMaster));
        }
        for (Node minion : targets) {
            if (!minion.getPrivateIp().equals(getGatewayPrivateIp())) {
                saltAction.addMinion(createMinion(minion));
            }
        }
        return saltAction;
    }

    private Minion createMinion(Node node) {
        Minion minion = new Minion();
        minion.setAddress(node.getPrivateIp());
        minion.setRoles(Collections.emptyList());
        minion.setServer(getGatewayPrivateIp());
        minion.setHostGroup(node.getHostGroup());
        minion.setDomain(domain);
        return minion;
    }

    private String getGatewayPrivateIp() {
        return gatewayConfig.getPrivateAddress();
    }

    @Override
    public String toString() {
        return "SaltBootstrap{"
                + "gatewayConfig=" + gatewayConfig
                + ", originalTargets=" + originalTargets
                + ", targets=" + targets
                + '}';
    }
}
