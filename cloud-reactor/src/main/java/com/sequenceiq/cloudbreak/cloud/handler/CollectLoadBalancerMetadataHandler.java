package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.handler.service.LoadBalancerMetadataService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CollectLoadBalancerMetadataHandler implements CloudPlatformEventHandler<CollectLoadBalancerMetadataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectLoadBalancerMetadataHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private LoadBalancerMetadataService loadBalancerMetadataService;

    @Override
    public Class<CollectLoadBalancerMetadataRequest> type() {
        return CollectLoadBalancerMetadataRequest.class;
    }

    @Override
    public void accept(Event<CollectLoadBalancerMetadataRequest> collectLBMetadataRequestEvent) {
        LOGGER.debug("Received event: {}", collectLBMetadataRequestEvent);
        CollectLoadBalancerMetadataRequest request = collectLBMetadataRequestEvent.getData();
        try {
            List<CloudLoadBalancerMetadata> loadBalancerStatuses = loadBalancerMetadataService.collectMetadata(request.getCloudContext(),
                request.getCloudCredential(), request.getTypesPresentInStack());
            CollectLoadBalancerMetadataResult collectLBMetadataResult =
                new CollectLoadBalancerMetadataResult(request.getResourceId(), loadBalancerStatuses);

            request.getResult().onNext(collectLBMetadataResult);
            eventBus.notify(collectLBMetadataResult.selector(), new Event<>(collectLBMetadataRequestEvent.getHeaders(), collectLBMetadataResult));
            LOGGER.debug("Load balancer metadata collection successfully finished");
        } catch (RuntimeException e) {
            LOGGER.error("Collecting metadata failed", e);
            CollectLoadBalancerMetadataResult failure = new CollectLoadBalancerMetadataResult(e, request.getResourceId());
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(collectLBMetadataRequestEvent.getHeaders(), failure));
        }
    }
}
