package com.sequenceiq.cloudbreak.cm.commands;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class SyncApiCommandPollerConfig {

    private static final String DEPLOY_CLUSTER_CLIENT_CONFIG_COMMAND_NAME = "DeployClusterClientConfig";

    private final Integer interruptTimeoutSeconds;

    private final EntitlementService entitlementService;

    public SyncApiCommandPollerConfig(
            @Value("${cb.cm.client.syncCommandPoller.interrupt.timeout.seconds:}") Integer interruptTimeoutSeconds,
            EntitlementService entitlementService) {
        this.interruptTimeoutSeconds = interruptTimeoutSeconds;
        this.entitlementService = entitlementService;
    }

    public Integer getInterruptTimeoutSeconds() {
        return interruptTimeoutSeconds;
    }

    public boolean isSyncApiCommandPollingEnaabled(String resourceCrn) {
        return entitlementService.useCmSyncCommandPoller(Crn.safeFromString(resourceCrn).getAccountId());
    }

    public String getDeployClusterClientConfigCommandName() {
        return DEPLOY_CLUSTER_CLIENT_CONFIG_COMMAND_NAME;
    }
}
