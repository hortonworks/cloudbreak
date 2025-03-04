package com.sequenceiq.freeipa.flow.freeipa.enableselinux.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxHandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaOrchestrationConfigService;
import com.sequenceiq.freeipa.service.stack.SeLinuxEnablementService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaEnableSeLinuxHandler extends ExceptionCatcherEventHandler<FreeIpaEnableSeLinuxHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaEnableSeLinuxHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private FreeIpaOrchestrationConfigService freeIpaOrchestrationConfigService;

    @Inject
    private SeLinuxEnablementService seLinuxEnablementService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaEnableSeLinuxHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaEnableSeLinuxHandlerEvent> event) {
        LOGGER.warn("Exception while trying to set SELinux to 'ENFORCING', exception: ", e);
        return new FreeIpaEnableSeLinuxFailedEvent(resourceId, "SELINUX_ENABLEMENT_FAILED", e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<FreeIpaEnableSeLinuxHandlerEvent> enableSeLinuxEventEvent) {
        FreeIpaEnableSeLinuxHandlerEvent eventData = enableSeLinuxEventEvent.getData();
        try {
            LOGGER.debug("Starting handler for setting SELinux to ENFORCING.");
            Stack stack = stackService.getByIdWithListsInTransaction(eventData.getResourceId());
            LOGGER.debug("Saving SeLinux as 'ENFORCING' in stack security config.");
            securityConfigService.updateSeLinuxSecurityConfig(stack.getSecurityConfig().getId(), SeLinux.ENFORCING);
            LOGGER.debug("Updating salt pillar properties based on stack.");
            freeIpaOrchestrationConfigService.configureOrchestrator(eventData.getResourceId());
            LOGGER.debug("Running role - selinux_enforce - on all FreeIPA instances.");
            seLinuxEnablementService.enableSeLinuxOnAllNodes(stack);
            LOGGER.debug("Finished updating selinux state on FreeIPA for stack.");
            return new FreeIpaEnableSeLinuxEvent(FreeIpaEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_FREEIPA_EVENT.selector(),
                    eventData.getResourceId(), eventData.getOperationId());
        } catch (Exception e) {
            return new FreeIpaEnableSeLinuxFailedEvent(eventData.getResourceId(), "SELINUX_ENABLEMENT_FAILED", e);
        }
    }
}
