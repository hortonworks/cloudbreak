package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreRequest;
import com.sequenceiq.freeipa.flow.stack.HealthCheckSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("RebuildCleanupFreeIpaAfterRestoreAction")
public class RebuildCleanupFreeIpaAfterRestoreAction extends AbstractRebuildAction<HealthCheckSuccess> {
    protected RebuildCleanupFreeIpaAfterRestoreAction() {
        super(HealthCheckSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, HealthCheckSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Cleanup FreeIPA after restore");
        sendEvent(context, new FreeIpaCleanupAfterRestoreRequest(payload.getResourceId()));
    }

    @Override
    protected Object getFailurePayload(HealthCheckSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaCleanupAfterRestoreFailed(payload.getResourceId(), ex, ERROR);
    }
}
