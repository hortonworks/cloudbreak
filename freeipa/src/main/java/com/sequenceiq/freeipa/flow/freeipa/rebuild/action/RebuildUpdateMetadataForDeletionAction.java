package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_METADATA_FOR_DELETION_FINISHED_EVENT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.downscale.action.FreeIpaDownscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

/**
 * TODO
 * Update instance metadate status
 *
 * @see FreeIpaDownscaleActions#updateMetadataForDeletionRequestAction()
 */
@Component("RebuildUpdateMetadataForDeletionAction")
public class RebuildUpdateMetadataForDeletionAction extends AbstractRebuildAction<StackEvent> {
    protected RebuildUpdateMetadataForDeletionAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new StackEvent(UPDATE_METADATA_FOR_DELETION_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
