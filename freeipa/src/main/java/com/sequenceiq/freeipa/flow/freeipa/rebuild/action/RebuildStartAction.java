package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.REBUILD_STARTED_EVENT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

@Component("RebuildStartAction")
public class RebuildStartAction extends AbstractRebuildAction<RebuildEvent> {
    protected RebuildStartAction() {
        super(RebuildEvent.class);
    }

    @Override
    protected void prepareExecution(RebuildEvent payload, Map<Object, Object> variables) {
        super.prepareExecution(payload, variables);
        setInstanceToRestoreFqdn(variables, payload.getInstanceToRestoreFqdn());
        setFullBackupStorageLocation(variables, payload.getFullBackupStorageLocation());
        setDataBackupStorageLocation(variables, payload.getDataBackupStorageLocation());
        setOperationId(variables, payload.getOperationId());
    }

    @Override
    protected void doExecute(StackContext context, RebuildEvent payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "FreeIPA rebuild requested");
        sendEvent(context, new StackEvent(REBUILD_STARTED_EVENT.event(), payload.getResourceId()));
    }
}
