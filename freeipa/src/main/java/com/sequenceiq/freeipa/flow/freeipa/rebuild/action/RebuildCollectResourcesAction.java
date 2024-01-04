package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.action.FreeIpaDownscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.resource.ResourceService;

/**
 * TODO
 * Collect resource to scale down
 *
 * @see FreeIpaDownscaleActions#collectResourcesAction()
 */
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
        List<CloudResource> cloudResources = resourceService.getAllCloudResource(context.getStack());
        // TODO check if this right
        List<CloudInstance> cloudInstances = context.getStack().getAllInstanceMetaDataList().stream()
                .filter(im -> !im.isTerminated())
                .map(instanceMetaData -> instanceConverter.convert(instanceMetaData))
                .collect(Collectors.toList());
        DownscaleStackCollectResourcesRequest request = new DownscaleStackCollectResourcesRequest(context.getCloudContext(),
                context.getCloudCredential(), context.getCloudStack(), cloudResources, cloudInstances);
        sendEvent(context, request.selector(), request);
    }
}
