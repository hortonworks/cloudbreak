package com.sequenceiq.cloudbreak.cm.commands;

import static com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants.DEPLOY_CLUSTER_CLIENT_CONFIG_COMMAND_NAME;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

@Component
public class SyncApiCommandPollerConfig {

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

    public boolean isSyncApiCommandPollingEnabled(String resourceCrn) {
        return entitlementService.useCmSyncCommandPoller(Crn.safeFromString(resourceCrn).getAccountId());
    }

    public String getDeployClusterClientConfigCommandName() {
        return DEPLOY_CLUSTER_CLIENT_CONFIG_COMMAND_NAME;
    }
}
