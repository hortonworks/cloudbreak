package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.CREATED;
import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.SAVE_METADATA_FINISHED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.stack.instance.MetadataSetupService;

/**
 * TODO
 * Save extra info from provider regarding instance
 *
 * @see FreeIpaUpscaleActions#saveMetadataAction()
 */
@Component("RebuildSaveMetadataAction")
public class RebuildSaveMetadataAction extends AbstractRebuildAction<CollectMetadataResult> {

    @Inject
    private MetadataSetupService metadataSetupService;

    protected RebuildSaveMetadataAction() {
        super(CollectMetadataResult.class);
    }

    @Override
    protected void doExecute(StackContext context, CollectMetadataResult payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Saving metadata");
        metadataSetupService.saveInstanceMetaData(context.getStack(), payload.getResults(), CREATED);
        sendEvent(context, SAVE_METADATA_FINISHED_EVENT.event(), new StackEvent(payload.getResourceId()));
    }
}
