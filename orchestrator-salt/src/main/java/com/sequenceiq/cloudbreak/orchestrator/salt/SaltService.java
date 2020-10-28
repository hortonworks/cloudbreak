package com.sequenceiq.cloudbreak.orchestrator.salt;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;

import io.opentracing.Tracer;

@Service
public class SaltService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltService.class);

    @Inject
    private SaltErrorResolver saltErrorResolver;

    @Inject
    private Tracer tracer;

    @Value("${rest.debug}")
    private boolean restDebug;

    public SaltConnector createSaltConnector(GatewayConfig gatewayConfig) {
        return new SaltConnector(gatewayConfig, saltErrorResolver, restDebug, tracer);
    }

    public GatewayConfig getPrimaryGatewayConfig(List<GatewayConfig> allGatewayConfigs) throws CloudbreakOrchestratorFailedException {
        Optional<GatewayConfig> gatewayConfigOptional = allGatewayConfigs.stream().filter(GatewayConfig::isPrimary).findFirst();
        if (gatewayConfigOptional.isPresent()) {
            GatewayConfig gatewayConfig = gatewayConfigOptional.get();
            LOGGER.debug("Primary gateway: {},", gatewayConfig);
            return gatewayConfig;
        }
        throw new CloudbreakOrchestratorFailedException("No primary gateway specified");
    }
}
