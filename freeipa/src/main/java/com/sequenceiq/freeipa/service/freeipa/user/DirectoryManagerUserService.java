package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

@Service
public class DirectoryManagerUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryManagerUserService.class);

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public void updateDirectoryManagerPassword(Stack stack, String newPassword) {
        stack.getPrimaryGateway()
                .ifPresent(pgwInstanceMetadata -> {
                    GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, pgwInstanceMetadata);
                    try {
                        FreeIpa freeIpa = freeIpaService.findByStack(stack);
                        String oldPassword = freeIpa.getAdminPassword();
                        Map<String, String> commandResult = hostOrchestrator.runCommandOnAllHosts(gatewayConfig,
                                String.format("export HOSTNAME=$(hostname -f);" +
                                        "dsconf -D \"cn=Directory Manager\" -w \"%s\" ldaps://$HOSTNAME config replace nsslapd-rootpw=\"%s\"",
                                        oldPassword, newPassword));
                        Map<String, String> failedHosts = new HashMap<>();
                        for (Map.Entry<String, String> resultEntry : commandResult.entrySet()) {
                            if (!resultEntry.getValue().contains("Successfully replaced")) {
                                failedHosts.put(resultEntry.getKey(), resultEntry.getValue());
                            }
                        }
                        if (!failedHosts.isEmpty()) {
                            throw new CloudbreakRuntimeException("Directory Manager password change failed on " + String.join(", ", failedHosts.keySet())
                                    + ". " + failedHosts);
                        }
                        LOGGER.info("Result: {}", commandResult);
                    } catch (CloudbreakOrchestratorFailedException e) {
                        throw new CloudbreakRuntimeException("Directory Manager password change failed", e);
                    }
                });
    }

    public void checkDirectoryManagerPassword(Stack stack) {
        stack.getPrimaryGateway()
                .ifPresent(pgwInstanceMetadata -> {
                    GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, pgwInstanceMetadata);
                    try {
                        FreeIpa freeIpa = freeIpaService.findByStack(stack);
                        String password = freeIpa.getAdminPassword();
                        Map<String, String> commandResult = hostOrchestrator.runCommandOnAllHosts(gatewayConfig,
                                String.format("export HOSTNAME=$(hostname -f);" +
                                                "dsconf -D \"cn=Directory Manager\" -w \"%s\" ldaps://$HOSTNAME config get", password));
                        Map<String, String> failedHosts = new HashMap<>();
                        for (Map.Entry<String, String> resultEntry : commandResult.entrySet()) {
                            if (!resultEntry.getValue().contains("nsslapd-port: 389")) {
                                failedHosts.put(resultEntry.getKey(), resultEntry.getValue());
                            }
                        }
                        if (!failedHosts.isEmpty()) {
                            throw new CloudbreakRuntimeException("Directory Manager password check failed on " + String.join(", ", failedHosts.keySet())
                                    + ". " + failedHosts);
                        }
                        LOGGER.info("Result: {}", commandResult);
                    } catch (CloudbreakOrchestratorFailedException e) {
                        throw new CloudbreakRuntimeException("Directory Manager password check failed", e);
                    }
                });
    }

}
