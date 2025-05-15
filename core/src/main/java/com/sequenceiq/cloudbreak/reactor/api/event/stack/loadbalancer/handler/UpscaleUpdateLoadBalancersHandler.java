package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.UPSCALE_UPDATE_USERDATA_SECRETS_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpscaleUpdateLoadBalancersFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpscaleUpdateLoadBalancersRequest;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpscaleUpdateLoadBalancersHandler extends ExceptionCatcherEventHandler<UpscaleUpdateLoadBalancersRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleUpdateLoadBalancersHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleUpdateLoadBalancersRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpscaleUpdateLoadBalancersRequest> event) {
        LOGGER.error("Unexpected error occurred while updating load balancers.", e);
        return new UpscaleUpdateLoadBalancersFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpscaleUpdateLoadBalancersRequest> event) {
        UpscaleUpdateLoadBalancersRequest request = event.getData();
        Long stackId = request.getResourceId();
        CloudStack cloudStack = request.getCloudStack();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();

        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
        try {
            LOGGER.info("Updating load balancers for stack {}", stackId);
            connector.resources().updateLoadBalancers(ac, cloudStack, persistenceNotifier);
        } catch (Exception e) {
            LOGGER.error("Failed to update load balancers", e);
            return new UpscaleUpdateLoadBalancersFailed(stackId, e);
        }
        return new StackEvent(UPSCALE_UPDATE_USERDATA_SECRETS_EVENT.selector(), stackId);
    }
}
