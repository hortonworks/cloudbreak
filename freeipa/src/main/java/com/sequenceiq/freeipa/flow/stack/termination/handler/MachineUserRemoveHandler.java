package com.sequenceiq.freeipa.flow.stack.termination.handler;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.termination.event.ums.RemoveMachineUserFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.ums.RemoveMachineUserRequest;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class MachineUserRemoveHandler implements EventHandler<RemoveMachineUserRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MachineUserRemoveHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveMachineUserRequest.class);
    }

    @Override
    public void accept(Event<RemoveMachineUserRequest> event) {
        RemoveMachineUserRequest request = event.getData();
        LOGGER.info("Cleanup machine user for logging from UMS (if it is needed)...");
        cleanupMachineUser(request.getResourceId());
        RemoveMachineUserFinished response = new RemoveMachineUserFinished(request.getResourceId(), request.getForced());
        eventBus.notify(EventSelectorUtil.selector(RemoveMachineUserFinished.class),
                new Event<>(event.getHeaders(), response));
    }

    private void cleanupMachineUser(Long stackId) {
        Stack stack = stackService.getStackById(stackId);
        Telemetry telemetry = stack.getTelemetry();
        if (telemetry != null && (telemetry.isClusterLogsCollectionEnabled() || StringUtils.isNotBlank(stack.getDatabusCredential()))) {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> altusMachineUserService.cleanupMachineUser(stack, telemetry));
        } else {
            LOGGER.info("Machine user cleanup is not needed.");
        }
    }
}
