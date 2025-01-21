package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataCloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataCloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.handler.service.LoadBalancerMetadataService;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class CollectLoadBalancerMetadataCloudPlatformHandler implements CloudPlatformEventHandler<CollectLoadBalancerMetadataCloudPlatformRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectLoadBalancerMetadataCloudPlatformHandler.class);

    @Inject
    private LoadBalancerMetadataService loadBalancerMetadataService;

    @Override
    public Class<CollectLoadBalancerMetadataCloudPlatformRequest> type() {
        return CollectLoadBalancerMetadataCloudPlatformRequest.class;
    }

    @Override
    public void accept(Event<CollectLoadBalancerMetadataCloudPlatformRequest> collectLoadBalancerMetadataRequest) {
        LOGGER.debug("Received event: {}", collectLoadBalancerMetadataRequest);
        CollectLoadBalancerMetadataCloudPlatformRequest request = collectLoadBalancerMetadataRequest.getData();
        try {
            List<CloudLoadBalancerMetadata> loadBalancerStatuses = loadBalancerMetadataService.collectMetadata(request.getCloudContext(),
                    request.getCloudCredential(), request.getTypesPresentInStack(), request.getCloudResources());
            CollectLoadBalancerMetadataCloudPlatformResult collectLBMetadataResult =
                    new CollectLoadBalancerMetadataCloudPlatformResult(request.getResourceId(), loadBalancerStatuses);

            request.getResult().onNext(collectLBMetadataResult);
            LOGGER.info("Load balancer metadata collection successfully finished");
        } catch (RuntimeException e) {
            LOGGER.warn("Collecting load balancer metadata failed", e);
            CollectLoadBalancerMetadataCloudPlatformResult failure = new CollectLoadBalancerMetadataCloudPlatformResult(e, request.getResourceId());
            request.getResult().onNext(failure);
        }
    }
}
