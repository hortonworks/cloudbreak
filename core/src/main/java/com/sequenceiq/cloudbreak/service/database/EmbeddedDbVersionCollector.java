package com.sequenceiq.cloudbreak.service.database;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Component
public class EmbeddedDbVersionCollector {

    private static final String UNKNOWN = "Unknown";

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDbVersionCollector.class);

    private static final String DB_VERSION_COMMAND =
            "{ psql -U postgres -c 'show server_version;' -t 2>/dev/null || echo " + UNKNOWN + "; } | { grep -o '[0-9]*\\.' || echo " + UNKNOWN + "; }";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public Optional<String> collectDbVersion(Stack stack) throws CloudbreakOrchestratorFailedException {
        List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Optional<String> primaryGwHostname = allGatewayConfigs.stream().filter(GatewayConfig::isPrimary).findFirst().map(GatewayConfig::getHostname);
        if (primaryGwHostname.isPresent()) {
            Map<String, String> commandResult = hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of(primaryGwHostname.get()), DB_VERSION_COMMAND);
            String rawVersion = commandResult.get(primaryGwHostname.get());
            LOGGER.debug("Raw postgresql version: [{}]", rawVersion);
            if (StringUtils.isBlank(rawVersion) || rawVersion.contains(UNKNOWN)) {
                LOGGER.warn("Couldn't get postgresql version");
                return Optional.empty();
            } else {
                return MajorVersion.get(rawVersion).map(MajorVersion::getMajorVersion);
            }
        } else {
            LOGGER.warn("No primary gateway found when trying to fetch postgresql version");
            return Optional.empty();
        }
    }
}
