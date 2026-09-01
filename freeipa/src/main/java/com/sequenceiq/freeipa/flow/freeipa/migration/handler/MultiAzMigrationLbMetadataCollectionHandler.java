package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbMetadataCollectionHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbMetadataCollectionResult;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerMetadataCollectionService;

@Component
public class MultiAzMigrationLbMetadataCollectionHandler extends ExceptionCatcherEventHandler<MultiAzMigrationLbMetadataCollectionHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationLbMetadataCollectionHandler.class);

    @Inject
    private FreeIpaLoadBalancerMetadataCollectionService freeIpaLoadBalancerMetadataCollectionService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(MultiAzMigrationLbMetadataCollectionHandlerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MultiAzMigrationLbMetadataCollectionHandlerRequest> event) {
        LOGGER.warn("Exception during multi-AZ migration load balancer metadata collection for stack {}: ", resourceId, e);
        return new MultiAzMigrationInitFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<MultiAzMigrationLbMetadataCollectionHandlerRequest> event) {
        MultiAzMigrationLbMetadataCollectionHandlerRequest request = event.getData();
        Long stackId = request.getResourceId();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        CloudStack cloudStack = request.getCloudStack();

        LOGGER.info("Refreshing load balancer metadata for stack: {}", stackId);
        freeIpaLoadBalancerMetadataCollectionService.collectLoadBalancerMetadata(
                new LoadBalancerMetadataCollectionRequest(stackId, cloudContext, cloudCredential, cloudStack));
        return new MultiAzMigrationLbMetadataCollectionResult(stackId, request.getOperationId());
    }
}
