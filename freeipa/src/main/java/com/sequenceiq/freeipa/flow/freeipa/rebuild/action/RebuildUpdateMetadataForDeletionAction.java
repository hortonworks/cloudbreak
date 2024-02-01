package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_METADATA_FOR_DELETION_FINISHED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.downscale.action.FreeIpaDownscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.termination.action.TerminationService;

/**
 * TODO
 * Update instance metadate status
 *
 * @see FreeIpaDownscaleActions#updateMetadataForDeletionRequestAction()
 */
@Component("RebuildUpdateMetadataForDeletionAction")
public class RebuildUpdateMetadataForDeletionAction extends AbstractRebuildAction<StackEvent> {

    @Inject
    private TerminationService terminationService;

    protected RebuildUpdateMetadataForDeletionAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Updating metadata for deletion request");
        terminationService.requestDeletion(payload.getResourceId(), null);
        sendEvent(context, new StackEvent(UPDATE_METADATA_FOR_DELETION_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
