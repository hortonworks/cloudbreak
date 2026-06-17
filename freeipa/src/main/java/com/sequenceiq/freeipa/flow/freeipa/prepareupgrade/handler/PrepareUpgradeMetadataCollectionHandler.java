package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeMetadataCollectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeMetadataCollectionSuccess;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerMetadataCollectionService;

@Component
public class PrepareUpgradeMetadataCollectionHandler extends ExceptionCatcherEventHandler<PrepareUpgradeMetadataCollectionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareUpgradeMetadataCollectionHandler.class);

    @Inject
    private FreeIpaLoadBalancerMetadataCollectionService freeIpaLoadBalancerMetadataCollectionService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PrepareUpgradeMetadataCollectionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<PrepareUpgradeMetadataCollectionRequest> event) {
        LOGGER.error("Unexpected error during prepare upgrade metadata collection", e);
        return new PrepareUpgradeFailureEvent(resourceId, VALIDATION, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<PrepareUpgradeMetadataCollectionRequest> event) {
        PrepareUpgradeMetadataCollectionRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            LOGGER.debug("Collecting load balancer metadata for prepare upgrade validation");
            freeIpaLoadBalancerMetadataCollectionService.collectLoadBalancerMetadata(
                    new LoadBalancerMetadataCollectionRequest(stackId, request.getCloudContext(),
                            request.getCloudCredential(), request.getCloudStack()));
            return new PrepareUpgradeMetadataCollectionSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Failed to collect load balancer metadata during prepare upgrade", e);
            return new PrepareUpgradeFailureEvent(stackId, VALIDATION, e);
        }
    }
}
