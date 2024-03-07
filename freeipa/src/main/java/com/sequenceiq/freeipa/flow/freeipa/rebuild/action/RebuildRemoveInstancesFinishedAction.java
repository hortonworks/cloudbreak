package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.ADD_INSTANCE_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.termination.action.TerminationService;

@Component("RebuildRemoveInstancesFinishedAction")
public class RebuildRemoveInstancesFinishedAction extends AbstractRebuildAction<DownscaleStackResult> {

    @Inject
    private TerminationService terminationService;

    protected RebuildRemoveInstancesFinishedAction() {
        super(DownscaleStackResult.class);
    }

    @Override
    protected void doExecute(StackContext context, DownscaleStackResult payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Finished removing instances");
        terminationService.terminateMetaDataInstances(context.getStack(), null);
        sendEvent(context, new StackEvent(ADD_INSTANCE_EVENT.event(), payload.getResourceId()));
    }
}
