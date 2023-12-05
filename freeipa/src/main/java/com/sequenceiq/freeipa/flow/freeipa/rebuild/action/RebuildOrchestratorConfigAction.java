package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;

/**
 * TODO
 * Push pillars
 *
 * @see FreeIpaUpscaleActions#orchestratorConfig()
 */
@Component("RebuildOrchestratorConfigAction")
public class RebuildOrchestratorConfigAction extends AbstractRebuildAction<BootstrapMachinesSuccess> {
    protected RebuildOrchestratorConfigAction() {
        super(BootstrapMachinesSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) throws Exception {
        OrchestratorConfigRequest request = new OrchestratorConfigRequest(payload.getResourceId());
        sendEvent(context, request);
    }
}
