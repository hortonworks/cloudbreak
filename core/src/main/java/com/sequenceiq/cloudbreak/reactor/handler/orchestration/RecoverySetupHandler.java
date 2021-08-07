package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SetupRecoveryFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SetupRecoveryRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SetupRecoverySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.service.recovery.RdsRecoverySetupService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class RecoverySetupHandler extends ExceptionCatcherEventHandler<SetupRecoveryRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoverySetupHandler.class);

    @Inject
    private RdsRecoverySetupService rdsRecoverySetupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SetupRecoveryRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SetupRecoveryRequest> event) {
        LOGGER.error("RecoverySetupHandler step failed with the following message: {}", e.getMessage());
        return new SetupRecoveryFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SetupRecoveryRequest> event) {
        Long stackId = event.getData().getResourceId();
        try {
            if (event.getData().getProvisionType() == ProvisionType.RECOVERY) {
                LOGGER.debug("Provision type is recovery, so running recovery setup state.");
                rdsRecoverySetupService.addRecoverRole(stackId);
            } else {
                LOGGER.debug("Provision type is not recovery, skipping recovery specific logic..");
            }
            return new SetupRecoverySuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Recovery setup failed", e);
            return new SetupRecoveryFailed(stackId, e);
        }
    }
}
