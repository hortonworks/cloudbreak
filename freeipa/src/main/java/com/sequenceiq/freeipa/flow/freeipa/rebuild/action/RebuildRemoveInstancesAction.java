package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Component("RebuildRemoveInstancesAction")
public class RebuildRemoveInstancesAction extends AbstractRebuildAction<DownscaleStackCollectResourcesResult> {

    @Inject
    private ResourceService resourceService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceConverter;

    protected RebuildRemoveInstancesAction() {
        super(DownscaleStackCollectResourcesResult.class);
    }

    @Override
    protected void doExecute(StackContext context, DownscaleStackCollectResourcesResult payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Decommissioning instances");
        List<CloudResource> cloudResources = resourceService.getAllCloudResource(context.getStack().getId());
        List<CloudInstance> cloudInstances = context.getStack().getAllInstanceMetaDataList().stream()
                .filter(im -> !im.isTerminated())
                .map(instanceMetaData -> instanceConverter.convert(instanceMetaData))
                .collect(Collectors.toList());
        DownscaleStackRequest request = new DownscaleStackRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                cloudResources, cloudInstances, payload.getResourcesToScale());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(DownscaleStackCollectResourcesResult payload, Optional<StackContext> flowContext, Exception ex) {
        return new DownscaleStackResult(ex.getMessage(), ex, payload.getResourceId());
    }
}
