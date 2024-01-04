package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

@Service
public class DirectoryManagerUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryManagerUserService.class);

    private static final String DSCONF_PASS_CHANGE_COMMAND = "export HOSTNAME=$(hostname -f);" +
            "dsconf -D \"cn=Directory Manager\" -w \"%s\" ldaps://$HOSTNAME config replace nsslapd-rootpw=\"%s\"";

    private static final String DSCONF_CONFIG_GET_COMMAND = "export HOSTNAME=$(hostname -f);" +
            "dsconf -D \"cn=Directory Manager\" -w \"%s\" ldaps://$HOSTNAME config get";

    private static final String DSCONF_PASS_CHANGE_SUCCESSFUL_RESULT = "Successfully replaced";

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    private Map<String, String> runCommand(Stack stack, String command, String successfulResponse) throws DirectoryManagerRunCommandFailedException {
        InstanceMetaData pgwInstanceMetadata = stack.getPrimaryGatewayAndThrowExceptionIfEmpty();
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, pgwInstanceMetadata);
        try {
            Map<String, String> commandResult = hostOrchestrator.runCommandOnAllHosts(gatewayConfig, command);
            LOGGER.info("Result: {}", commandResult);
            Map<String, String> failedHosts = new HashMap<>();
            for (Map.Entry<String, String> resultEntry : commandResult.entrySet()) {
                if (!resultEntry.getValue().contains(successfulResponse)) {
                    failedHosts.put(resultEntry.getKey(), resultEntry.getValue());
                }
            }
            return failedHosts;
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Directory Manager command failed", e);
            throw new DirectoryManagerRunCommandFailedException("Directory Manager command failed", e);
        }
    }

    public void updateDirectoryManagerPassword(Stack stack, String newPassword) throws DirectoryManagerPasswordUpdateFailedException {
        LOGGER.info("Update directory manager password for: {}", stack.getResourceCrn());
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        String oldPassword = freeIpa.getAdminPassword();
        String passChangeCommand = String.format(DSCONF_PASS_CHANGE_COMMAND, oldPassword, newPassword);
        try {
            Map<String, String> failedHosts = runCommand(stack, passChangeCommand, DSCONF_PASS_CHANGE_SUCCESSFUL_RESULT);
            if (!failedHosts.isEmpty()) {
                LOGGER.error("Failed hosts for update directory manager password: {}", failedHosts);
                throw new DirectoryManagerPasswordUpdateFailedException("Directory Manager password change failed on " + String.join(", ", failedHosts.keySet())
                        + ". " + failedHosts);
            }
        } catch (DirectoryManagerRunCommandFailedException e) {
            throw new DirectoryManagerPasswordUpdateFailedException("Directory Manager password change failed", e.getCause());
        }
    }

    public void checkDirectoryManagerPassword(Stack stack) throws DirectoryManagerPasswordCheckFailedException {
        LOGGER.info("Check if directory manager password works for: {}", stack.getResourceCrn());
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        String password = freeIpa.getAdminPassword();
        try {
            Map<String, String> failedHosts = runCommand(stack, String.format(DSCONF_CONFIG_GET_COMMAND, password), "nsslapd-port: 389");
            if (!failedHosts.isEmpty()) {
                LOGGER.error("Failed hosts for directory manager password check: {}", failedHosts);
                throw new DirectoryManagerPasswordCheckFailedException("Directory Manager password check failed on " + String.join(", ", failedHosts.keySet())
                        + ". " + failedHosts);
            }
        } catch (DirectoryManagerRunCommandFailedException e) {
            throw new DirectoryManagerPasswordCheckFailedException("Directory Manager password check failed", e.getCause());
        }
    }
}
