package com.sequenceiq.cloudbreak.orchestrator.salt;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.GatewayConfigComparator;

@Service
public class SaltService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltService.class);

    @Inject
    private SaltErrorResolver saltErrorResolver;

    @Value("${rest.debug}")
    private boolean restDebug;

    public SaltConnector createSaltConnector(GatewayConfig gatewayConfig) {
        return new SaltConnector(gatewayConfig, saltErrorResolver, restDebug);
    }

    public SaltConnector createSaltConnector(GatewayConfig gatewayConfig, int connectTimeoutMs, int readTimeout) {
        return new SaltConnector(gatewayConfig, saltErrorResolver, restDebug, connectTimeoutMs, OptionalInt.of(readTimeout));
    }

    public List<SaltConnector> createSaltConnector(Collection<GatewayConfig> gatewayConfigs) {
        return gatewayConfigs.stream().map(this::createSaltConnector).collect(Collectors.toList());
    }

    /**
     * Returns the primary gateway config for use during Salt communication in FMS. If a minion has a newer Salt version, it will be selected
     * instead of the original PGW. This is required for the Salt version upgrade, where the old minions can connect to a new master, but a new minion
     * cannot connect to an old master, so we have to ensure to always connect to the gateway with the latest salt version.
     * @return primary gateway config for salt communication
     */
    public GatewayConfig getPrimaryGatewayConfig(List<GatewayConfig> allGatewayConfigs) throws CloudbreakOrchestratorFailedException {
        Optional<GatewayConfig> originalPrimaryGatewayConfig = allGatewayConfigs.stream().filter(GatewayConfig::isPrimary).findFirst();
        if (originalPrimaryGatewayConfig.isEmpty()) {
            throw new CloudbreakOrchestratorFailedException("No primary gateway specified");
        }
        GatewayConfig overriddenPrimaryGatewayConfig = allGatewayConfigs.stream()
                .sorted(new GatewayConfigComparator())
                .findFirst()
                .orElse(originalPrimaryGatewayConfig.get());
        if (!originalPrimaryGatewayConfig.get().getInstanceId().equals(overriddenPrimaryGatewayConfig.getInstanceId())) {
            LOGGER.info("Override original primary gateway config {} and use instance with the latest salt version: {}",
                    originalPrimaryGatewayConfig.get().getInstanceId(), overriddenPrimaryGatewayConfig.getInstanceId());
        }
        LOGGER.debug("Primary gateway: {},", overriddenPrimaryGatewayConfig);
        return overriddenPrimaryGatewayConfig;
    }

}


