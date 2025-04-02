package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_CORE_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.SeLinuxEnablementService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CoreEnableSeLinuxHandler extends ExceptionCatcherEventHandler<CoreEnableSeLinuxHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreEnableSeLinuxHandler.class);

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
        return EventSelectorUtil.selector(CoreEnableSeLinuxHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CoreEnableSeLinuxHandlerEvent> event) {
        return new CoreEnableSeLinuxFailedEvent(resourceId, "SELINUX_ENABLEMENT_FAILED", e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<CoreEnableSeLinuxHandlerEvent> enableSeLinuxEventEvent) {
        CoreEnableSeLinuxHandlerEvent eventData = enableSeLinuxEventEvent.getData();
        try {
            LOGGER.debug("Setting SELinux mode to 'ENFORCING'.");
            Stack stack = stackService.getByIdWithListsInTransaction(eventData.getResourceId());
            LOGGER.debug("Saving SeLinux as 'ENFORCING' in stack security config.");
            securityConfigService.updateSeLinuxSecurityConfig(stack.getSecurityConfig().getId(), SeLinux.ENFORCING);
            LOGGER.debug("Updating salt pillar properties based on stack.");
            clusterHostServiceRunner.updateClusterConfigs(stackService.getStackProxyById(stack.getId()), true);
            LOGGER.debug("Running role - selinux_update - on all instances.");
            seLinuxEnablementService.enableSeLinuxOnAllNodes(stack);
            LOGGER.debug("Finished updating selinux state on for stack.");
            return new CoreEnableSeLinuxEvent(FINISH_ENABLE_SELINUX_CORE_EVENT.selector(), eventData.getResourceId());
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Exception while setting SELinux to ENFORCING mode, exception: ", e);
            return new CoreEnableSeLinuxFailedEvent(eventData.getResourceId(), "SELINUX_ENABLEMENT_FAILED", e);
        }
    }
}
