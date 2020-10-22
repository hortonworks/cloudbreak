package com.sequenceiq.cloudbreak.cloud.azure.task.dnszone;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class AzureDnsZoneCreationPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDnsZoneCreationPoller.class);

    @Value("${cb.azure.poller.dns.checkinterval:2000}")
    private int creationCheckInterval;

    @Value("${cb.azure.poller.dns.maxattempt:60}")
    private int creationCheckMaxAttempt;

    @Value("${cb.azure.poller.dns.maxfailurenumber:5}")
    private int maxTolerableFailureNumber;

    @Inject
    private AzurePollTaskFactory azurePollTaskFactory;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    public void startPolling(AuthenticatedContext ac, AzureDnsZoneCreationCheckerContext checkerContext) {
        PollTask<Boolean> dnsZoneCreationCheckerTask = azurePollTaskFactory.dnsZoneCreationCheckerTask(ac, checkerContext);
        try {
            LOGGER.info("Start polling dns zone and network link creation: {}", checkerContext.getDeploymentName());
            syncPollingScheduler.schedule(dnsZoneCreationCheckerTask, creationCheckInterval,
                    creationCheckMaxAttempt, maxTolerableFailureNumber);
        } catch (Exception e) {
            LOGGER.error("Dns zone and network link creation failed.", e);
            throw new CloudConnectorException(e);
        }
    }
}
