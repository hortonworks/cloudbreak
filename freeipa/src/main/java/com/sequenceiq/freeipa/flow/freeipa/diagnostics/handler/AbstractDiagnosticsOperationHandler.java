package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsFlowException;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionFailureEvent;

import reactor.bus.Event;

public abstract class AbstractDiagnosticsOperationHandler extends ExceptionCatcherEventHandler<DiagnosticsCollectionEvent> {

    @Override
    protected Selectable doAccept(HandlerEvent<DiagnosticsCollectionEvent> event) {
        try {
            return executeOperation(event);
        } catch (Exception e) {
            throw new DiagnosticsFlowException(String.format("Error during diagnostics operation: %s%n%s", getOperationName(), e.getMessage()), e);
        }
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DiagnosticsCollectionEvent> event) {
        return new DiagnosticsCollectionFailureEvent(resourceId, e, event.getData().getResourceCrn(), event.getData().getParameters(), getFailureType().name());
    }

    public abstract Selectable executeOperation(HandlerEvent<DiagnosticsCollectionEvent> data) throws Exception;

    public abstract UsageProto.CDPVMDiagnosticsFailureType.Value getFailureType();

    public abstract String getOperationName();
}
