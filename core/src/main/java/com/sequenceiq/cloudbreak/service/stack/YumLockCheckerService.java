package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.RetryType;

@Component
public class YumLockCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YumLockCheckerService.class);

    private static final String COMMAND = "yum list installed cdp-telemetry";

    private static final String ERROR_RPMDB_OPEN_FAILED = "Error: rpmdb open failed";

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void validate(StackDto stack) {
        try {
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            GatewayConfig primaryGatewayConfig = gatewayConfigs.stream().filter(GatewayConfig::isPrimary).findFirst().orElseThrow();
            Map<String, String> yumOutput = hostOrchestrator.runCommandOnAllHosts(primaryGatewayConfig, COMMAND, RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES);
            String hostnamesYumNotWorking = yumOutput.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(ERROR_RPMDB_OPEN_FAILED))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(","));
            if (StringUtils.isNotBlank(hostnamesYumNotWorking)) {
                throw new CloudbreakRuntimeException(String.format("Operaton cannot be performed " +
                        "because the yum database is locked on the following host(s): %s.\n" +
                        "Please try removing the lock files and rpm databases on these machines and retry the operation:\n" +
                        "rm /var/lib/rpm/.dbenv.lock\n" +
                        "rm /var/lib/rpm/.rpm.lock\n" +
                        "rm /var/lib/rpm/__db* ", hostnamesYumNotWorking));
            }
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.debug("Ignoring error during yum lock checking: {}", e);
        } catch (CloudbreakRuntimeException e) {
            LOGGER.debug("Validation error during yum lock check.", e);
            throw e;
        }
    }
}
