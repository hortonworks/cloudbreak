package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.common.api.type.LoadBalancerType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateCloudLoadBalancersFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateCloudLoadBalancersRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateCloudLoadBalancersSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class CreateCloudLoadBalancersHandler extends ExceptionCatcherEventHandler<CreateCloudLoadBalancersRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCloudLoadBalancersHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CreateCloudLoadBalancersRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CreateCloudLoadBalancersRequest> event) {
        return new CreateCloudLoadBalancersFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        CreateCloudLoadBalancersRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            LOGGER.info("Updating cloud stack with load balancer network information");
            CloudStack origCloudStack = request.getCloudStack();
            Stack stack = request.getStack();
            CloudStack updatedCloudStack = new CloudStack(
                origCloudStack.getGroups(),
                cloudStackConverter.buildNetwork(stack),
                origCloudStack.getImage(),
                origCloudStack.getParameters(),
                origCloudStack.getTags(),
                origCloudStack.getTemplate(),
                origCloudStack.getInstanceAuthentication(),
                origCloudStack.getLoginUserName(),
                origCloudStack.getPublicKey(),
                origCloudStack.getFileSystem().orElse(null),
                origCloudStack.getLoadBalancers()
            );

            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            LOGGER.debug("Initiating cloud load balancer creation");
            List<CloudResourceStatus> resourceStatus = connector.resources().updateLoadBalancers(ac, updatedCloudStack, persistenceNotifier);
            if (resourceStatus.stream().anyMatch(CloudResourceStatus::isFailed)) {
                Set<String> names = resourceStatus.stream()
                    .filter(CloudResourceStatus::isFailed)
                    .map(r -> r.getCloudResource().getName())
                    .collect(Collectors.toSet());
                throw new CloudbreakException("Creation failed for load balancers: " + names);
            }

            Set<String> types = updatedCloudStack.getLoadBalancers().stream()
                .map(CloudLoadBalancer::getType)
                .map(LoadBalancerType::toString)
                .collect(Collectors.toSet());
            LOGGER.info("Cloud load balancer creation for load balancer types {} successful", types);
            return new CreateCloudLoadBalancersSuccess(stack);
        } catch (Exception e) {
            LOGGER.warn("Failed to created cloud load balance resources.", e);
            return new CreateCloudLoadBalancersFailure(request.getResourceId(), e);
        }
    }
}
