package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Component("RebuildCollectResourcesAction")
public class RebuildCollectResourcesAction extends AbstractRebuildAction<StackEvent> {

    @Inject
    private ResourceService resourceService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceConverter;

    protected RebuildCollectResourcesAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Collecting resources");
        List<CloudResource> cloudResources = resourceService.getAllCloudResource(context.getStack().getId());
        List<CloudInstance> cloudInstances = context.getStack().getAllInstanceMetaDataList().stream()
                .filter(im -> !im.isTerminated())
                .map(instanceMetaData -> instanceConverter.convert(instanceMetaData))
                .collect(Collectors.toList());
        DownscaleStackCollectResourcesRequest request = new DownscaleStackCollectResourcesRequest(context.getCloudContext(),
                context.getCloudCredential(), context.getCloudStack(), cloudResources, cloudInstances);
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception e) {
        return new DownscaleStackCollectResourcesResult(e.getMessage(), e, payload.getResourceId());
    }
}
