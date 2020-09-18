package com.sequenceiq.cloudbreak.cloud.azure.task.image;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class AzureManagedImageCreationPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureManagedImageCreationPoller.class);

    private static final int MANAGED_IMAGE_CREATION_CHECKING_INTERVAL = 1000;

    private static final int MANAGED_IMAGE_CREATION_CHECKING_MAX_ATTEMPT = 100;

    private static final int MAX_FAILURE_TOLERANT = 5;

    @Inject
    private AzurePollTaskFactory azurePollTaskFactory;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    public void startPolling(AuthenticatedContext ac, AzureManagedImageCreationCheckerContext checkerContext) {
        PollTask<Boolean> managedImageCreationStatusCheckerTask = azurePollTaskFactory.managedImageCreationCheckerTask(ac, checkerContext);
        try {
            LOGGER.info("Start polling managed image creation: {}", checkerContext.getImageName());
            syncPollingScheduler.schedule(managedImageCreationStatusCheckerTask, MANAGED_IMAGE_CREATION_CHECKING_INTERVAL,
                    MANAGED_IMAGE_CREATION_CHECKING_MAX_ATTEMPT, MAX_FAILURE_TOLERANT);
        } catch (Exception e) {
            LOGGER.error("Managed image creation failed.", e);
            throw new CloudConnectorException(e);
        }
    }
}
