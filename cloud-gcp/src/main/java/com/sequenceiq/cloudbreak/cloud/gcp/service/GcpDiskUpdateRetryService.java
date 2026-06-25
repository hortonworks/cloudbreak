package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.DisksResizeRequest;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.AbstractGcpComputeBaseResourceChecker;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ResourceChecker;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpDiskUpdateRetryService extends AbstractGcpComputeBaseResourceChecker implements ResourceChecker<GcpContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDiskUpdateRetryService.class);

    @Inject
    private ResourcePollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Retryable(retryFor = GoogleJsonResponseException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public List<CloudResourceStatus> resizeDisk(GcpResizeDiskParameters params, GcpContext gcpContext) throws Exception {
        String diskDeviceName = params.diskName();
        LOGGER.info("Resizing GCP disk {} in zone {} to {} GB.", diskDeviceName, params.preferredZone(), params.size());
        DisksResizeRequest resizeRequest = new DisksResizeRequest().setSizeGb((long) params.size());
        Operation resizeOperation = params.compute().disks().resize(params.projectId(), params.preferredZone(), diskDeviceName, resizeRequest).execute();
        if (resizeOperation.getHttpErrorStatusCode() != null) {
            throw new CloudbreakServiceException(String.format("Resize request for GCP disk %s failed: %s",
                    diskDeviceName, resizeOperation.getHttpErrorMessage()));
        }
        CloudResource operationAwareCloudResource = createOperationAwareCloudResource(params.cloudResource(), resizeOperation);
        LOGGER.debug("Waiting for resize operation {} of GCP disk {} to finish.", resizeOperation.getName(), diskDeviceName);
        List<CloudResourceStatus> result = waitForOperation(params.authenticatedContext(), gcpContext, List.of(operationAwareCloudResource));
        LOGGER.info("Successfully resized GCP disk {} in zone {} to {} GB.", diskDeviceName, params.preferredZone(), params.size());
        return result;
    }

    @Override
    public List<CloudResourceStatus> checkResources(GcpContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(ResourceType.GCP_ATTACHED_DISKSET, context, auth, resources);
    }

    private List<CloudResourceStatus> waitForOperation(AuthenticatedContext authenticatedContext, GcpContext context, List<CloudResource> resources)
            throws Exception {
        PollTask<List<CloudResourceStatus>> task = statusCheckFactory.newPollResourceTask(this, authenticatedContext, resources, context, true);
        return syncPollingScheduler.schedule(task);
    }
}
