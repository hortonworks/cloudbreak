package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.PrivateIdProvider;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

/**
 * TODO
 * Create new instancemetadata for the instance to be restored and send request to cloud module to create instance
 *
 * @see FreeIpaUpscaleActions#addInstancesAction()
 */
@Component("RebuildAddInstanceAction")
public class RebuildAddInstanceAction extends AbstractRebuildAction<StackEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildAddInstanceAction.class);

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceService resourceService;

    @Inject
    private PrivateIdProvider privateIdProvider;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    protected RebuildAddInstanceAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Create new instance");
        String instanceToRestoreFqdn = getInstanceToRestoreFqdn(variables);
        List<CloudInstance> newInstances = buildNewInstance(context.getStack(), instanceToRestoreFqdn);
        addNewInstances(context, newInstances);
    }

    private void addNewInstances(StackContext context, List<CloudInstance> newInstances) {
        Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(context.getStack(), newInstances, List.of());
        List<CloudResource> cloudResources = resourceService.getAllCloudResource(context.getStack());
        CloudStack updatedCloudStack = cloudStackConverter.convert(updatedStack);
        UpscaleStackRequest<UpscaleStackResult> request = new UpscaleStackRequest<>(
                context.getCloudContext(), context.getCloudCredential(), updatedCloudStack, cloudResources,
                new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L));
        sendEvent(context, request.selector(), request);
    }

    private List<CloudInstance> buildNewInstance(Stack stack, String instanceToRestoreFqdn) {
        long privateId = stack.getAllInstanceMetaDataList().stream()
                .filter(im -> instanceToRestoreFqdn.equals(im.getDiscoveryFQDN()))
                .map(InstanceMetaData::getPrivateId)
                .filter(Objects::nonNull)
                .findFirst().orElseGet(() -> privateIdProvider.getFirstValidPrivateId(stack.getInstanceGroups()));
        InstanceMetaData instanceMetaDataWithFqdn = new InstanceMetaData();
        instanceMetaDataWithFqdn.setDiscoveryFQDN(instanceToRestoreFqdn);
        instanceMetaDataWithFqdn.setPrivateId(privateId);
        InstanceGroup instanceGroup = stack.getInstanceGroups().stream().findFirst().orElseThrow();
        return List.of(cloudStackConverter.buildInstance(stack, instanceMetaDataWithFqdn, instanceGroup, stack.getStackAuthentication(), privateId,
                InstanceStatus.CREATE_REQUESTED));
    }
}
