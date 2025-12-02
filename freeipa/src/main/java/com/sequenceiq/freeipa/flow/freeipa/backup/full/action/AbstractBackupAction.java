package com.sequenceiq.freeipa.flow.freeipa.backup.full.action;

import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent.FULL_BACKUP_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.BackupContext;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupState;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractBackupAction<P extends Payload> extends AbstractAction<FullBackupState, FullBackupEvent, BackupContext, P>
        implements OperationAwareAction, FlowChainAwareAction {

    @Inject
    private StackService stackService;

    protected AbstractBackupAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected BackupContext createFlowContext(FlowParameters flowParameters, StateContext<FullBackupState, FullBackupEvent> stateContext, P payload) {
        Stack stack = stackService.getStackById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        return new BackupContext(flowParameters, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<BackupContext> flowContext, Exception ex) {
        return new StackFailureEvent(FULL_BACKUP_FAILED_EVENT.event(), payload.getResourceId(), ex, ERROR);
    }
}
