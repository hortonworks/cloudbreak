package com.sequenceiq.cloudbreak.cloud.azure.task.database;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class AzureDatabaseTemplateDeploymentPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseTemplateDeploymentPoller.class);

    @Value("${cb.azure.poller.template.deployment.checkinterval:10000}")
    private int deploymentCheckInterval;

    @Value("${cb.azure.poller.template.deployment.maxattempt:120}")
    private int deploymentCheckMaxAttempt;

    @Value("${cb.azure.poller.template.deployment.maxfailurenumber:5}")
    private int maxTolerableFailureNumber;

    @Inject
    private AzurePollTaskFactory azurePollTaskFactory;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    public void startPolling(AuthenticatedContext ac, AzureDatabaseTemplateDeploymentContext deploymentContext) throws Exception {
        PollTask<Boolean> templateDeploymentPollTask = azurePollTaskFactory.databaseCreationPollTask(ac, deploymentContext);
        LOGGER.info("Start polling template deployment: {}", deploymentContext.getAzureTemplateDeploymentParameters().getTemplateName());
        syncPollingScheduler.schedule(templateDeploymentPollTask, deploymentCheckInterval, deploymentCheckMaxAttempt, maxTolerableFailureNumber);
    }

}