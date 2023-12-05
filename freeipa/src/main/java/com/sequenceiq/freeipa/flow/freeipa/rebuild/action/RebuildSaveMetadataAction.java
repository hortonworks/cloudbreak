package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.SAVE_METADATA_FINISHED_EVENT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

/**
 * TODO
 * Save extra info from provider regarding instance
 *
 * @see FreeIpaUpscaleActions#saveMetadataAction()
 */
@Component("RebuildSaveMetadataAction")
public class RebuildSaveMetadataAction extends AbstractRebuildAction<CollectMetadataResult> {
    protected RebuildSaveMetadataAction() {
        super(CollectMetadataResult.class);
    }

    @Override
    protected void doExecute(StackContext context, CollectMetadataResult payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, SAVE_METADATA_FINISHED_EVENT.event(), new StackEvent(payload.getResourceId()));
    }
}
