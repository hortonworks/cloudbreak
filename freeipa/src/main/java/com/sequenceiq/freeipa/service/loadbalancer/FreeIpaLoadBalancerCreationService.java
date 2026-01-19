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
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision.LoadBalancerProvisionRequest;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Service
public class FreeIpaLoadBalancerCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerCreationService.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private ResourceService resourceService;

    public void createLoadBalancer(LoadBalancerProvisionRequest request) {
        try {
            LOGGER.debug("Starting FreeIPA load balancer provision.");
            CloudContext cloudContext = request.getCloudContext();
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatform(), cloudContext.getVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            if (loadBalancerProvisionRequired(request.getResourceId())) {
                List<CloudResourceStatus> loadBalancerResourceStatus = connector
                        .resources()
                        .launchLoadBalancers(ac, request.getCloudStack(), persistenceNotifier);
                waitForResources(ac, loadBalancerResourceStatus, cloudContext);
            }
            LOGGER.debug("FreeIPA load balancer provision finished.");
        } catch (Exception e) {
            LOGGER.error("FreeIPA load balancer provision failed.", e);
            throw new CloudbreakServiceException(e);
        }
    }

    private boolean loadBalancerProvisionRequired(Long stackId) {
        return resourceService.findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus.CREATED, ResourceType.GCP_BACKEND_SERVICE, stackId)
                .stream()
                .findFirst()
                .isEmpty();
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
