package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
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
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateCloudLoadBalancersFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateCloudLoadBalancersRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateCloudLoadBalancersSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CreateCloudLoadBalancersHandler extends ExceptionCatcherEventHandler<CreateCloudLoadBalancersRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCloudLoadBalancersHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CreateCloudLoadBalancersRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CreateCloudLoadBalancersRequest> event) {
        return new CreateCloudLoadBalancersFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CreateCloudLoadBalancersRequest> event) {
        CreateCloudLoadBalancersRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            LOGGER.info("Updating cloud stack with load balancer network information");
            CloudStack origCloudStack = request.getCloudStack();

            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            LOGGER.debug("Initiating cloud load balancer creation");
            List<CloudResourceStatus> resourceStatus = connector.resources().launchLoadBalancers(ac, origCloudStack, persistenceNotifier);

            LOGGER.debug("Waiting for cloud load balancers to be fully created");
            List<CloudResource> resources = ResourceLists.transform(resourceStatus);
            PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
            ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatus);
            if (!task.completed(statePollerResult)) {
                syncPollingScheduler.schedule(task);
            }

            if (resourceStatus.stream().anyMatch(CloudResourceStatus::isFailed)) {
                Set<String> names = resourceStatus.stream()
                    .filter(CloudResourceStatus::isFailed)
                    .map(r -> r.getCloudResource().getName())
                    .collect(Collectors.toSet());
                throw new CloudbreakException("Creation failed for load balancers: " + names);
            }

            Set<String> types = origCloudStack.getLoadBalancers().stream()
                .map(CloudLoadBalancer::getType)
                .map(LoadBalancerType::toString)
                .collect(Collectors.toSet());
            LOGGER.info("Cloud load balancer creation for load balancer types {} successful", types);
            return new CreateCloudLoadBalancersSuccess(cloudContext.getId());
        } catch (Exception e) {
            LOGGER.warn("Failed to created cloud load balance resources.", e);
            return new CreateCloudLoadBalancersFailure(request.getResourceId(), e);
        }
    }
}
