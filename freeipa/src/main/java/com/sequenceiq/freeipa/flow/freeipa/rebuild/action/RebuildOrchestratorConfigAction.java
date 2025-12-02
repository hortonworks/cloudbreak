package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("RebuildOrchestratorConfigAction")
public class RebuildOrchestratorConfigAction extends AbstractRebuildAction<BootstrapMachinesSuccess> {
    protected RebuildOrchestratorConfigAction() {
        super(BootstrapMachinesSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Configuring the orchestrator");
        OrchestratorConfigRequest request = new OrchestratorConfigRequest(payload.getResourceId());
        sendEvent(context, request);
    }

    @Override
    protected Object getFailurePayload(BootstrapMachinesSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new OrchestratorConfigFailed(payload.getResourceId(), ex, ERROR);
    }
}