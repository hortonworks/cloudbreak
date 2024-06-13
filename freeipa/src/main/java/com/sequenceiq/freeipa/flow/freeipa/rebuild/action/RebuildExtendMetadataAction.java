package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Component("RebuildExtendMetadataAction")
public class RebuildExtendMetadataAction extends AbstractRebuildAction<StackEvent> {

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    protected RebuildExtendMetadataAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Extending metadata");
        List<CloudResource> cloudResources = resourceService.getAllCloudResource(context.getStack());
        List<CloudInstance> allKnownInstances = cloudStackConverter.buildInstances(context.getStack());
        CollectMetadataRequest request = new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources,
                allKnownInstances, allKnownInstances);
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception ex) {
        return new CollectMetadataResult(ex, payload.getResourceId());
    }
}
