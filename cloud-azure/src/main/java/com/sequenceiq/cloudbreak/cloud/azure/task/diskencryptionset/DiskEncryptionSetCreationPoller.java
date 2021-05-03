package com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class DiskEncryptionSetCreationPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskEncryptionSetCreationPoller.class);

    @Value("${cb.azure.poller.des.checkinterval:1000}")
    private int creationCheckInterval;

    @Value("${cb.azure.poller.des.maxattempt:30}")
    private int creationCheckMaxAttempt;

    @Value("${cb.azure.poller.des.maxfailurenumber:5}")
    private int maxTolerableFailureNumber;

    @Inject
    private AzurePollTaskFactory azurePollTaskFactory;

    @Inject
    private SyncPollingScheduler<DiskEncryptionSetInner> syncPollingScheduler;

    @Inject
    private AzureUtils azureUtils;

    public DiskEncryptionSetInner startPolling(AuthenticatedContext authenticatedContext, DiskEncryptionSetCreationCheckerContext checkerContext,
            DiskEncryptionSetInner desInitial) {
        try {
            PollTask<DiskEncryptionSetInner> checkerTask =
                    azurePollTaskFactory.diskEncryptionSetCreationCheckerTask(requireNonNull(authenticatedContext), requireNonNull(checkerContext));
            String resourceGroupName = checkerContext.getResourceGroupName();
            String diskEncryptionSetName = checkerContext.getDiskEncryptionSetName();
            DiskEncryptionSetInner result = desInitial;

            if (checkerTask.completed(result)) {
                LOGGER.info("Creation of Disk Encryption Set \"{}\" in Resource Group \"{}\" has already completed.", diskEncryptionSetName, resourceGroupName);
            } else {
                LOGGER.info("Start polling the creation of Disk Encryption Set \"{}\" in Resource Group \"{}\".", diskEncryptionSetName, resourceGroupName);
                result = syncPollingScheduler.schedule(checkerTask, creationCheckInterval, creationCheckMaxAttempt, maxTolerableFailureNumber);
                LOGGER.info("Polling finished, creation of Disk Encryption Set \"{}\" in Resource Group \"{}\" is complete.", diskEncryptionSetName,
                        resourceGroupName);
            }

            return result;
        } catch (Exception e) {
            LOGGER.error("Disk Encryption Set creation failed, context=" + checkerContext, e);
            throw azureUtils.convertToCloudConnectorException(e, "Disk Encryption Set creation");
        }
    }

}
