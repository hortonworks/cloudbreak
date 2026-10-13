package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayService;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.replication.ChangePrimaryGatewayVerifyReplicationFailed;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.replication.ChangePrimaryGatewayVerifyReplicationRequest;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.replication.ChangePrimaryGatewayVerifyReplicationSuccess;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class VerifyReplicationConvergenceHandler extends ExceptionCatcherEventHandler<ChangePrimaryGatewayVerifyReplicationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyReplicationConvergenceHandler.class);

    @Inject
    private ChangePrimaryGatewayService changePrimaryGatewayService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ChangePrimaryGatewayVerifyReplicationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ChangePrimaryGatewayVerifyReplicationRequest> event) {
        return new ChangePrimaryGatewayVerifyReplicationFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ChangePrimaryGatewayVerifyReplicationRequest> event) {
        Long stackId = event.getData().getResourceId();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            changePrimaryGatewayService.verifyReplicationConvergenceOnPrimaryGateway(stack);
            return new ChangePrimaryGatewayVerifyReplicationSuccess(stackId);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("FreeIPA replication convergence verification failed after changing the primary gateway", e);
            return defaultFailureEvent(stackId, e, event.getEvent());
        }
    }
}
