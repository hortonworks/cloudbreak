package com.sequenceiq.freeipa.service.loadbalancer;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateRequest;

@Service
public class FreeIpaLoadBalancerUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerUpdateService.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    public void updateLoadBalancer(LoadBalancerUpdateRequest request) {
        try {
            LOGGER.debug("Starting FreeIPA load balancer update.");
            CloudContext cloudContext = request.getCloudContext();
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatform(), cloudContext.getVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudResourceStatus> loadBalancerResourceStatus = connector.resources().updateLoadBalancers(ac, request.getCloudStack(), persistenceNotifier);
            waitForResources(ac, loadBalancerResourceStatus, cloudContext);
            LOGGER.debug("FreeIPA load balancer update finished.");
        } catch (Exception e) {
            LOGGER.error("FreeIPA load balancer update failed.", e);
            throw new CloudbreakServiceException(e);
        }
    }

    private void waitForResources(AuthenticatedContext ac, List<CloudResourceStatus> resourceStatuses, CloudContext cloudContext) throws Exception {
        List<CloudResource> resources = ResourceLists.transform(resourceStatuses);
        PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
        ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatuses);
        if (!task.completed(statePollerResult)) {
            syncPollingScheduler.schedule(task);
        }
    }
}
