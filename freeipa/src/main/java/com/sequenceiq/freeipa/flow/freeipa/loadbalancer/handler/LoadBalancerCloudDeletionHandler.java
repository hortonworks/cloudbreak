package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerDeletionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerCloudDeletionRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerCloudDeletionSuccess;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Component
public class LoadBalancerCloudDeletionHandler extends ExceptionCatcherEventHandler<LoadBalancerCloudDeletionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerCloudDeletionHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(LoadBalancerCloudDeletionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<LoadBalancerCloudDeletionRequest> event) {
        return new LoadBalancerDeletionFailureEvent(resourceId, FailureType.ERROR, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<LoadBalancerCloudDeletionRequest> event) {
        LoadBalancerCloudDeletionRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            LOGGER.debug("Deleting FreeIPA load balancer cloud resources for stack {}", stackId);
            CloudContext cloudContext = request.getCloudContext();
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatform(), cloudContext.getVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());

            Set<ResourceType> lbResourceTypes = ResourceType.getLbResourceTypes(cloudContext.getPlatform().value());
            List<Resource> lbResources = resourceService.findAllByStackIdAndResourceTypeIn(stackId, lbResourceTypes);
            List<CloudResource> lbCloudResources = lbResources.stream()
                    .map(resourceToCloudResourceConverter::convert)
                    .toList();

            LOGGER.debug("Deleting {} load balancer(s) from cloud for stack {}: {}", lbCloudResources.size(), stackId, lbCloudResources);
            connector.resources().deleteLoadBalancers(ac, request.getCloudStack(), lbCloudResources);
            resourceService.deleteAll(lbResources);
            freeIpaLoadBalancerService.delete(stackId);
            return new LoadBalancerCloudDeletionSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Failed to delete FreeIPA load balancer cloud resources for stack {}", stackId, e);
            return new LoadBalancerDeletionFailureEvent(stackId, FailureType.ERROR, e);
        }
    }
}
