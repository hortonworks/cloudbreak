package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbWaitHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbWaitResult;

@Component
public class MultiAzMigrationLbWaitHandler extends ExceptionCatcherEventHandler<MultiAzMigrationLbWaitHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationLbWaitHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(MultiAzMigrationLbWaitHandlerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MultiAzMigrationLbWaitHandlerRequest> event) {
        LOGGER.warn("Exception during multi-AZ migration load balancer wait for stack {}: ", resourceId, e);
        return new MultiAzMigrationInitFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<MultiAzMigrationLbWaitHandlerRequest> event) {
        MultiAzMigrationLbWaitHandlerRequest request = event.getData();
        Long stackId = request.getResourceId();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        CloudStack cloudStack = request.getCloudStack();

        LOGGER.info("Waiting for load balancer to be ready for stack: {}", stackId);
        CloudConnector cloudConnector = cloudPlatformConnectors.get(new CloudPlatformVariant(cloudContext.getPlatform(), cloudContext.getVariant()));
        AuthenticatedContext ac = cloudConnector.authentication().authenticate(cloudContext, cloudCredential);

        cloudConnector.resources().waitForLoadBalancers(ac, cloudStack);

        return new MultiAzMigrationLbWaitResult(stackId, request.getOperationId());
    }
}
