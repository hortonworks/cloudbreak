package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.resource.ResourceService;

/**
 * TODO
 * Create new instancemetadata for the instance to be restored and send request to cloud module to create instance
 *
 * @see FreeIpaUpscaleActions#addInstancesAction()
 */
@Component("RebuildAddInstanceAction")
public class RebuildAddInstanceAction extends AbstractRebuildAction<DownscaleStackResult> {
    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceService resourceService;

    protected RebuildAddInstanceAction() {
        super(DownscaleStackResult.class);
    }

    @Override
    protected void doExecute(StackContext context, DownscaleStackResult payload, Map<Object, Object> variables) throws Exception {
        List<CloudResource> cloudResources = resourceService.getAllCloudResource(context.getStack());
        // TODO update stack with creating a single IM
        CloudStack updatedCloudStack = cloudStackConverter.convert(context.getStack());
        UpscaleStackRequest<UpscaleStackResult> request = new UpscaleStackRequest<>(
                context.getCloudContext(), context.getCloudCredential(), updatedCloudStack, cloudResources,
                new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L));
        sendEvent(context, request.selector(), request);
    }
}
