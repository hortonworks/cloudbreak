package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.service.LoadBalancerMetadataService;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.LoadBalancerMetadataFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.LoadBalancerMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.LoadBalancerMetadataSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class LoadBalancerMetadataHandler extends ExceptionCatcherEventHandler<LoadBalancerMetadataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerMetadataHandler.class);

    @Inject
    private LoadBalancerMetadataService loadBalancerMetadataService;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(LoadBalancerMetadataRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<LoadBalancerMetadataRequest> event) {
        return new LoadBalancerMetadataFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<LoadBalancerMetadataRequest> event) {
        LoadBalancerMetadataRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        StackView stack = stackDtoService.getStackViewById(request.getResourceId());
        try {
            LOGGER.info("Fetch cloud load balancer metadata");
            List<CloudLoadBalancerMetadata> loadBalancerStatuses = loadBalancerMetadataService.collectMetadata(cloudContext,
                request.getCloudCredential(), request.getTypesPresentInStack(), request.getCloudResources());

            LOGGER.info("Persisting load balancer metadata to the database: {}", loadBalancerStatuses);
            metadataSetupService.saveLoadBalancerMetadata(stack, loadBalancerStatuses);

            LOGGER.info("Load balancer metadata collection was successful");
            return new LoadBalancerMetadataSuccess(stack.getId());
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch cloud load balancer metadata.", e);
            return new LoadBalancerMetadataFailure(request.getResourceId(), e);
        }
    }
}
