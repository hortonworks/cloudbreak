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
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbProvisionRequest;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbProvisionSuccess;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerCreationService;

@Component
public class PrepareUpgradeLbProvisionHandler extends ExceptionCatcherEventHandler<PrepareUpgradeLbProvisionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareUpgradeLbProvisionHandler.class);

    @Inject
    private FreeIpaLoadBalancerCreationService freeIpaLoadBalancerCreationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PrepareUpgradeLbProvisionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<PrepareUpgradeLbProvisionRequest> event) {
        LOGGER.error("Unexpected error during prepare upgrade LB provisioning", e);
        return new PrepareUpgradeFailureEvent(resourceId, VALIDATION, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<PrepareUpgradeLbProvisionRequest> event) {
        PrepareUpgradeLbProvisionRequest request = event.getData();
        Long stackId = request.getResourceId();
        try {
            LOGGER.debug("Provisioning temporary load balancer for prepare upgrade validation");
            freeIpaLoadBalancerCreationService.createLoadBalancer(
                    new com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision.LoadBalancerProvisionRequest(
                            stackId, request.getCloudContext(), request.getCloudCredential(), request.getCloudStack()));
            return new PrepareUpgradeLbProvisionSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Failed to provision temporary load balancer for prepare upgrade", e);
            return new PrepareUpgradeFailureEvent(stackId, VALIDATION, e);
        }
    }
}
