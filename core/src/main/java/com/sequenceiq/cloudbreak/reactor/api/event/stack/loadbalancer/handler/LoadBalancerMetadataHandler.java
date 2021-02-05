package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.handler.service.LoadBalancerMetadataService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.LoadBalancerMetadataFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.LoadBalancerMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.LoadBalancerMetadataSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class LoadBalancerMetadataHandler extends ExceptionCatcherEventHandler<LoadBalancerMetadataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerMetadataHandler.class);

    @Inject
    private LoadBalancerMetadataService loadBalancerMetadataService;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(LoadBalancerMetadataRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<LoadBalancerMetadataRequest> event) {
        return new LoadBalancerMetadataFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        LoadBalancerMetadataRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            LOGGER.info("Fetch cloud load balancer metadata");
            List<CloudLoadBalancerMetadata> loadBalancerStatuses = loadBalancerMetadataService.collectMetadata(cloudContext,
                request.getCloudCredential(), request.getTypesPresentInStack());

            Set<CloudLoadBalancerMetadata> failedStatues = loadBalancerStatuses.stream()
                .filter(this::isMissingMetadata)
                .collect(Collectors.toSet());
            if (!failedStatues.isEmpty()) {
                Set<String> names = failedStatues.stream()
                    .map(CloudLoadBalancerMetadata::getName)
                    .collect(Collectors.toSet());
                throw new CloudbreakException("Creation failed for load balancers: " + names);
            }

            LOGGER.info("Persisting load balancer metadata to the database: {}", loadBalancerStatuses);
            metadataSetupService.saveLoadBalancerMetadata(request.getStack(), loadBalancerStatuses);

            LOGGER.info("Load balancer metadata collection was successful");
            return new LoadBalancerMetadataSuccess(request.getStack());
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch cloud load balancer metadata.", e);
            return new LoadBalancerMetadataFailure(request.getResourceId(), e);
        }
    }

    private boolean isMissingMetadata(CloudLoadBalancerMetadata metadataStatus) {
        return metadataStatus.getType() == null ||
            !(StringUtils.isNotEmpty(metadataStatus.getIp()) ||
            (StringUtils.isNotEmpty(metadataStatus.getCloudDns()) && StringUtils.isNotEmpty(metadataStatus.getHostedZoneId())));
    }
}
