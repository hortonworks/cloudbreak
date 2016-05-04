package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltBootResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltBootResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.*;

public class SaltBootstrap implements OrchestratorBootstrap {


    private static final Logger LOGGER = LoggerFactory.getLogger(SaltBootstrap.class);

    private final SaltConnector sc;
    private final GatewayConfig gatewayConfig;
    private final Set<String> consulServers;
    private final Set<String> originalTargets;
    private Set<String> targets;

    public SaltBootstrap(SaltConnector sc, GatewayConfig gatewayConfig,
                         Set<String> targets, Set<String> consulServers) {
        this.sc = sc;
        this.gatewayConfig = gatewayConfig;
        this.originalTargets = Collections.unmodifiableSet(targets);
        this.targets = targets;
        this.consulServers = consulServers;
    }

    @Override
    public Boolean call() throws Exception {

        if (!targets.isEmpty()) {
            LOGGER.info("No missing targets for SaltBootstrap");

            SaltAction saltAction = createBootstrap();
            SaltBootResponses responses = sc.action(saltAction);

            Set<String> failedTargets = new HashSet<>();

            LOGGER.info("Salt run response: {}", responses);
            for (SaltBootResponse saltBootResponse : responses.getResponses()) {
                if (saltBootResponse.getStatusCode() != HttpStatus.OK.value()) {
                    LOGGER.info("Successfully distributed salt run to: " + saltBootResponse.getAddress());
                    failedTargets.add(saltBootResponse.getAddress().split(":")[0]);
                }
            }
            targets = failedTargets;

            if (!targets.isEmpty()) {
                LOGGER.info("Missing nodes to run salt: %s", targets);
                throw new CloudbreakOrchestratorFailedException("There are missing nodes from salt: " + targets);
            }
        }


        Map<String, Boolean> results = SaltStates.ping(sc, Glob.ALL).getResult().get(0);
        // TODO This is not correct in case of upscale
        if (originalTargets.size() > results.size()) {
            throw new CloudbreakOrchestratorFailedException("There are missing nodes from salt: " + results);
        }
        return true;
    }

    private SaltAction createBootstrap() {
        SaltAction saltAction = new SaltAction(SaltActionType.RUN);

        if (targets.contains(getGatewayPrivateIp())) {
            saltAction.setServer(getGatewayPrivateIp());
            List<String> roles = new ArrayList<>();
            roles.add("ambari_server");
            roles = appendConsulRole(getGatewayPrivateIp(), roles);
            saltAction.addMinion(createMinion(getGatewayPrivateIp(), roles));
        }
        for (String minionIp : targets) {
            if (!minionIp.equals(getGatewayPrivateIp())) {
                List<String> roles = new ArrayList<>();
                roles.add("ambari_agent");
                roles = appendConsulRole(getGatewayPrivateIp(), roles);
                saltAction.addMinion(createMinion(minionIp, roles));
            }
        }
        return saltAction;
    }

    private List<String> appendConsulRole(String minionIp, List<String> roles) {
        if (consulServers.contains(minionIp)) {
            roles.add("consul_server");
        } else {
            roles.add("consul_agent");
        }
        return roles;
    }

    private Minion createMinion(String address, List<String> roles) {
        Minion minion = new Minion();
        minion.setAddress(address);
        minion.setRoles(roles);
        minion.setServer(getGatewayPrivateIp());
        return minion;
    }

    private String getGatewayPrivateIp() {
        return gatewayConfig.getPrivateAddress();
    }

    public String getGatewayPublicIp() {
        return gatewayConfig.getPublicAddress();
    }
}
