package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.resource.ResourceService;

/**
 * TODO
 * Fetch extra info from provider regarding instance
 *
 * @see FreeIpaUpscaleActions#extendMetadataAction()
 */
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
        List<CloudResource> cloudResources = resourceService.getAllCloudResource(context.getStack());
        List<CloudInstance> allKnownInstances = cloudStackConverter.buildInstances(context.getStack());
        CollectMetadataRequest request = new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources,
                allKnownInstances, allKnownInstances);
        sendEvent(context, request.selector(), request);
    }
}
