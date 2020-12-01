package com.sequenceiq.cloudbreak.cloud.azure.task.image;

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
public class AzureManagedImageCreationPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureManagedImageCreationPoller.class);

    @Value("${cb.azure.poller.image.checkinterval:5000}")
    private int creationCheckInterval;

    @Value("${cb.azure.poller.image.maxattempt:60}")
    private int creationCheckMaxAttempt;

    @Value("${cb.azure.poller.image.maxfailurenumber:5}")
    private int maxTolerableFailureNumber;

    @Inject
    private AzurePollTaskFactory azurePollTaskFactory;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    public void startPolling(AuthenticatedContext ac, AzureManagedImageCreationCheckerContext checkerContext) throws Exception {
        PollTask<Boolean> managedImageCreationStatusCheckerTask = azurePollTaskFactory.managedImageCreationCheckerTask(ac, checkerContext);
        LOGGER.info("Start polling managed image creation: {}", checkerContext.getAzureImageInfo().getImageNameWithRegion());
            syncPollingScheduler.schedule(managedImageCreationStatusCheckerTask, creationCheckInterval,
                    creationCheckMaxAttempt, maxTolerableFailureNumber);
    }
}
