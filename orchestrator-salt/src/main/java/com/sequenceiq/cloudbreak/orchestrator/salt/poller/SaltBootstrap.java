package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.join.AcceptAllFpMatcher;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.join.DummyFingerprintCollector;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.join.EqualMinionFpMatcher;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.join.FingerprintFromSbCollector;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.join.MinionAcceptor;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.MinionUtil;

public class SaltBootstrap implements OrchestratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltBootstrap.class);

    private final SaltConnector sc;

    private final Collection<SaltConnector> saltConnectors;

    private final List<GatewayConfig> allGatewayConfigs;

    private final Set<Node> originalTargets;

    private final BootstrapParams params;

    private final SaltStateService saltStateService;

    private final MinionUtil minionUtil;

    private Set<Node> targets;

    /*
        Intentionally Package-private to be able to verify construction later.
        PowerMockito checked for the creation of the SaltBootsrap class. On regular Mockito, verifying of constructor call is not possible.
        To work around this, a Factory class is introduced that instantiates SaltBootstrap and now the factory method can be verified.
     */
    SaltBootstrap(SaltStateService saltStateService, MinionUtil minionUtil, SaltConnector sc, Collection<SaltConnector> saltConnectors,
            List<GatewayConfig> allGatewayConfigs, Set<Node> targets, BootstrapParams params) {
        this.sc = sc;
        this.saltConnectors = saltConnectors;
        this.allGatewayConfigs = allGatewayConfigs;
        originalTargets = Collections.unmodifiableSet(targets);
        this.targets = targets;
        this.params = params;
        this.saltStateService = saltStateService;
        this.minionUtil = minionUtil;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.debug("Bootstrapping of nodes [{}/{}]", originalTargets.size() - targets.size(), originalTargets.size());
        if (!targets.isEmpty()) {
            LOGGER.debug("Missing targets for SaltBootstrap: {}", targets);

            GenericResponses responses = saltStateService.bootstrap(sc, params, allGatewayConfigs, targets);

            Set<Node> failedTargets = new HashSet<>();

            LOGGER.debug("SaltBootstrap responses: {}", responses);
            for (GenericResponse genericResponse : responses.getResponses()) {
                if (genericResponse.getStatusCode() != HttpStatus.OK.value()) {
                    LOGGER.info("Failed to distributed salt run to: {}, error: {}", genericResponse.getAddress(), genericResponse.getErrorText());
                    String address = genericResponse.getAddress().split(":")[0];
                    failedTargets.addAll(originalTargets.stream().filter(a -> a.getPrivateIp().equals(address)).collect(Collectors.toList()));
                }
            }

            targets = failedTargets;

            if (!targets.isEmpty()) {
                LOGGER.info("Missing nodes to run saltbootstrap: {}", targets);
                throw new CloudbreakOrchestratorFailedException("There are missing nodes from saltbootstrap: " + targets);
            }

            if (params.isRestartNeeded()) {
                params.setRestartNeeded(false);
            }

            try {
                createMinionAcceptor().acceptMinions();
            } catch (CloudbreakOrchestratorFailedException e) {
                handleMinionAcceptingError(e);
            }
        }

        MinionIpAddressesResponse minionIpAddressesResponse = saltStateService.collectMinionIpAddresses(sc);
        if (minionIpAddressesResponse != null) {
            originalTargets.forEach(node -> {
                if (!minionIpAddressesResponse.getAllIpAddresses().contains(node.getPrivateIp())) {
                    LOGGER.info("Salt-minion is not responding on host: {}, yet", node);
                    targets.add(node);
                }
            });
        } else {
            throw new CloudbreakOrchestratorFailedException("Minions ip address collection returned null value from " + sc.getHostname());
        }

        if (!targets.isEmpty()) {
            throw new CloudbreakOrchestratorFailedException("There are missing nodes from salt network response: " + targets);
        }
        LOGGER.debug("Bootstrapping of nodes completed: {}", originalTargets.size());
        return true;
    }

    private void handleMinionAcceptingError(CloudbreakOrchestratorFailedException e) throws CloudbreakOrchestratorFailedException {
        if (e.getNodesWithErrors().isEmpty()) {
            targets = originalTargets;
        } else {
            Set<Node> nodesWithError = e.getNodesWithErrors().keySet().stream()
                    .filter(StringUtils::isNotBlank)
                    .map(nodeName ->
                            originalTargets.stream()
                                    .filter(node -> nodeName.equalsIgnoreCase(node.getHostname() + "." + node.getDomain()))
                                    .findFirst())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            targets = nodesWithError.isEmpty() ? originalTargets : nodesWithError;
        }
        throw e;
    }

    protected MinionAcceptor createMinionAcceptor() {
        List<Minion> minions = createMinionsFromOriginalTargets();
        return params.isSaltBootstrapFpSupported() ?
                new MinionAcceptor(saltConnectors, minions, new EqualMinionFpMatcher(), new FingerprintFromSbCollector())
                : new MinionAcceptor(saltConnectors, minions, new AcceptAllFpMatcher(), new DummyFingerprintCollector());
    }

    private List<Minion> createMinionsFromOriginalTargets() {
        List<String> gatewayPrivateIps = getGatewayPrivateIps();
        return originalTargets.stream()
                .map(ot -> minionUtil.createMinion(ot, gatewayPrivateIps, params.isRestartNeededFlagSupported(), params.isRestartNeeded()))
                .collect(Collectors.toList());
    }

    private List<String> getGatewayPrivateIps() {
        return allGatewayConfigs.stream().map(GatewayConfig::getPrivateAddress).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SaltBootstrap{");
        sb.append("sc=").append(sc);
        sb.append(", allGatewayConfigs=").append(allGatewayConfigs);
        sb.append(", originalTargets=").append(originalTargets);
        sb.append(", targets=").append(targets);
        sb.append('}');
        return sb.toString();
    }
}
