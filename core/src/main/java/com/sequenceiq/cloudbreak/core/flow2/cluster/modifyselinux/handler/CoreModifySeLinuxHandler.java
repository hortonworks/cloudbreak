package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.SeLinuxEnablementService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CoreModifySeLinuxHandler extends ExceptionCatcherEventHandler<CoreModifySeLinuxHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreModifySeLinuxHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private SeLinuxEnablementService seLinuxEnablementService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CoreModifySeLinuxHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CoreModifySeLinuxHandlerEvent> event) {
        return new CoreModifySeLinuxFailedEvent(resourceId, "SELINUX_MODE_CHANGE_FAILED", e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<CoreModifySeLinuxHandlerEvent> enableSeLinuxEventEvent) {
        CoreModifySeLinuxHandlerEvent eventData = enableSeLinuxEventEvent.getData();
        try {
            LOGGER.debug("Setting SELinux mode to " + eventData.getSelinuxMode());
            Stack stack = stackService.getByIdWithListsInTransaction(eventData.getResourceId());
            LOGGER.debug("Saving SeLinux as {} in stack security config.", eventData.getSelinuxMode());
            securityConfigService.updateSeLinuxSecurityConfig(stack.getSecurityConfig().getId(), eventData.getSelinuxMode());
            LOGGER.debug("Updating salt pillar properties based on stack.");
            clusterHostServiceRunner.updateClusterConfigs(stackService.getStackProxyById(stack.getId()), true);
            LOGGER.debug("Running role - selinux_update - on all instances.");
            seLinuxEnablementService.modifySeLinuxOnAllNodes(stack);
            LOGGER.debug("Finished updating selinux state on for stack.");
            return new CoreModifySeLinuxEvent(CoreModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_CORE_EVENT.selector(), eventData.getResourceId(),
                    eventData.getSelinuxMode());
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Exception while setting SELinux to " + eventData.getSelinuxMode() + ", exception: ", e);
            return new CoreModifySeLinuxFailedEvent(eventData.getResourceId(), "SELINUX_MODE_CHANGE_FAILED", e);
        }
    }
}
