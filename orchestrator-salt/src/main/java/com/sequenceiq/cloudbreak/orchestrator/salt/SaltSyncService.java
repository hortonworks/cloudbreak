package com.sequenceiq.cloudbreak.orchestrator.salt;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

@Service
public class SaltSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltSyncService.class);

    private static final int PROXY_TIMEOUT_MS = 10000;

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private static final int READ_TIMEOUT_MS = 5000;

    @Inject
    private SaltStateService saltStateService;

    @Inject
    private SaltService saltService;

    @Measure(SaltSyncService.class)
    public Optional<Set<String>> checkSaltMinions(GatewayConfig gatewayConfig) {
        try (SaltConnector sc = saltService.createSaltConnectorWithCustomTimeout(gatewayConfig,
                CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, PROXY_TIMEOUT_MS)) {
            MinionStatusSaltResponse minionStatusSaltResponse = saltStateService.collectNodeStatusWithLimitedRetry(sc);
            if (minionStatusSaltResponse.downMinions().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new HashSet<>(minionStatusSaltResponse.downMinions()));
        } catch (Exception e) {
            LOGGER.error("Error occurred during check of salt minions.", e);
            return Optional.of(Set.of(gatewayConfig.getHostname()));
        }
    }
}
