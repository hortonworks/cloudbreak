package com.sequenceiq.freeipa.flow.freeipa.enableselinux.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

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
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxHandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaOrchestrationConfigService;
import com.sequenceiq.freeipa.service.stack.SeLinuxModificationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaModifySeLinuxHandler extends ExceptionCatcherEventHandler<FreeIpaModifySeLinuxHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaModifySeLinuxHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private FreeIpaOrchestrationConfigService freeIpaOrchestrationConfigService;

    @Inject
    private SeLinuxModificationService seLinuxModificationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FreeIpaModifySeLinuxHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<FreeIpaModifySeLinuxHandlerEvent> event) {
        LOGGER.warn("Exception while trying to update SELinux to {}, exception: ", event.getData().getSeLinuxMode(), e);
        return new FreeIpaModifySeLinuxFailedEvent(resourceId, "SELINUX_MODIFICATION_FAILED", e, ERROR);
    }

    @Override
    public Selectable doAccept(HandlerEvent<FreeIpaModifySeLinuxHandlerEvent> enableSeLinuxEventEvent) {
        FreeIpaModifySeLinuxHandlerEvent eventData = enableSeLinuxEventEvent.getData();
        SeLinux selinuxMode = eventData.getSeLinuxMode();
        try {
            LOGGER.debug("Starting handler for setting SELinux to {}.", selinuxMode);
            Stack stack = stackService.getByIdWithListsInTransaction(eventData.getResourceId());
            LOGGER.debug("Saving SeLinux as {} in stack security config.", selinuxMode);
            securityConfigService.updateSeLinuxSecurityConfig(stack.getSecurityConfig().getId(), selinuxMode);
            LOGGER.debug("Updating salt pillar properties based on stack.");
            freeIpaOrchestrationConfigService.configureOrchestrator(eventData.getResourceId());
            LOGGER.debug("Running role - selinux_enforce - on all FreeIPA instances.");
            seLinuxModificationService.modifySeLinuxOnAllNodes(stack);
            LOGGER.debug("Finished updating selinux state on FreeIPA for stack.");
            return new FreeIpaModifySeLinuxEvent(FreeIpaModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_FREEIPA_EVENT.selector(),
                    eventData.getResourceId(), eventData.getOperationId(), selinuxMode);
        } catch (Exception e) {
            return new FreeIpaModifySeLinuxFailedEvent(eventData.getResourceId(), "SELINUX_MODIFICATION_FAILED", e, ERROR);
        }
    }
}
