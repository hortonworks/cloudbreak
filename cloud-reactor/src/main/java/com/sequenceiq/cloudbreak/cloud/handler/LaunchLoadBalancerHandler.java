package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchLoadBalancerRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchLoadBalancerResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class LaunchLoadBalancerHandler implements CloudPlatformEventHandler<LaunchLoadBalancerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchLoadBalancerHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<LaunchLoadBalancerRequest> type() {
        return LaunchLoadBalancerRequest.class;
    }

    @Override
    public void accept(Event<LaunchLoadBalancerRequest> launchLoadBalancerRequestEvent) {
        LOGGER.debug("Received event: {}", launchLoadBalancerRequestEvent);
        LaunchLoadBalancerRequest request = launchLoadBalancerRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        CloudStack cloudStack = request.getCloudStack();
        CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
        try {
            LaunchLoadBalancerResult result;
            if (!cloudStack.getLoadBalancers().isEmpty()) {
                ResourcesStatePollerResult statePollerResult = launchLoadBalancers(cloudStack, connector, ac,
                    persistenceNotifier, cloudContext);
                result = ResourcesStatePollerResults.transformToLaunchLoadBalancerResult(request, statePollerResult);
                request.getResult().onNext(result);
                LOGGER.debug("Launching the load balancers successfully finished for {}", cloudContext);
            } else {
                LOGGER.debug("No load balancers configured for stack.");
                result = new LaunchLoadBalancerResult(request.getResourceId(), List.of());
            }
            eventBus.notify(result.selector(), new Event<>(launchLoadBalancerRequestEvent.getHeaders(), result));
        } catch (Exception e) {
            if (ExceptionUtils.getRootCause(e) instanceof InterruptedException) {
                LOGGER.info("Interrupted exception is ignored as it has been thrown because of graceful shutdown of the java process.");
            }
            LaunchLoadBalancerResult failure = new LaunchLoadBalancerResult(e, request.getResourceId());
            LOGGER.warn("Error during launching the load balancerse:", e);
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(launchLoadBalancerRequestEvent.getHeaders(), failure));
        }
    }

    /**
     * Launches the desired load balancers for the stack using the cloud provider resource connector.
     * Adds the results of the launch to the existing state poller result from the launch of the main stack instances.
     */
    private ResourcesStatePollerResult launchLoadBalancers(CloudStack cloudStack, CloudConnector<Object> connector,
            AuthenticatedContext authenticatedContext, PersistenceNotifier persistenceNotifier, CloudContext cloudContext) throws Exception {
        List<CloudResourceStatus> loadBalancerResourceStatus = connector.resources().launchLoadBalancers(authenticatedContext, cloudStack,
            persistenceNotifier);
        return waitForResources(authenticatedContext, loadBalancerResourceStatus, cloudContext);
    }

    /**
     * Creates a poll task which waits for the resources to be fully up and running, or more specifically,
     * for the resources to be in a permanent successful state.
     *
     * Returns the result of the poll task.
     */
    private ResourcesStatePollerResult waitForResources(AuthenticatedContext ac, List<CloudResourceStatus> resourceStatuses, CloudContext cloudContext)
        throws Exception {
        List<CloudResource> resources = ResourceLists.transform(resourceStatuses);
        PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
        ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatuses);
        if (!task.completed(statePollerResult)) {
            statePollerResult = syncPollingScheduler.schedule(task);
        }
        return statePollerResult;
    }
}
