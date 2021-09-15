package com.sequenceiq.freeipa.flow.freeipa.downscale.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.AbstractCommonChainAction;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractDownscaleAction<P extends Payload> extends AbstractCommonChainAction<DownscaleState, DownscaleFlowEvent, StackContext, P> {

    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceConverter;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceConverter;

    protected AbstractDownscaleAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<DownscaleState, DownscaleFlowEvent> stateContext,
            P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        addMdcOperationIdIfPresent(stateContext.getExtendedState().getVariables());
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformvariant())
                .withLocation(location)
                .withUserName(stack.getOwner())
                .withAccountId(stack.getAccountId())
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new DownscaleFailureEvent(payload.getResourceId(), "Unexpected error during downscale action", Set.of(), Map.of(), ex);
    }

    protected List<InstanceMetaData> getInstanceMetadataFromStack(Stack stack, List<String> instanceIds) {
        return stack.getAllInstanceMetaDataList().stream()
                .filter(instanceMetaData -> instanceIds.contains(instanceMetaData.getInstanceId()))
                .collect(Collectors.toList());
    }

    protected List<CloudResource> getCloudResources(Stack stack) {
        return resourceService.findAllByStackId(stack.getId()).stream()
                .map(resource -> resourceConverter.convert(resource))
                .collect(Collectors.toList());
    }

    protected List<CloudInstance> getCloudInstances(Stack stack, List<String> instanceIds) {
        return getInstanceMetadataFromStack(stack, instanceIds).stream()
                .map(instanceMetaData -> instanceConverter.convert(instanceMetaData))
                .collect(Collectors.toList());
    }

    protected List<CloudInstance> getNonTerminatedCloudInstances(Stack stack, List<String> instanceIds) {
        // Exclude terminated but include deleted
        return getInstanceMetadataFromStack(stack, instanceIds).stream()
                .filter(im -> !im.isTerminated())
                .map(instanceMetaData -> instanceConverter.convert(instanceMetaData))
                .collect(Collectors.toList());
    }

    protected DetailedStackStatus getInProgressStatus(Map<Object, Object> variables) {
        DetailedStackStatus stackStatus;
        if (isRepair(variables)) {
            stackStatus = DetailedStackStatus.REPAIR_IN_PROGRESS;
        } else {
            stackStatus = DetailedStackStatus.DOWNSCALE_IN_PROGRESS;
        }
        return stackStatus;
    }

    protected DetailedStackStatus getDownscaleCompleteStatus(Map<Object, Object> variables) {
        DetailedStackStatus stackStatus;
        if (isRepair(variables)) {
            stackStatus = DetailedStackStatus.REPAIR_IN_PROGRESS;
        } else {
            stackStatus = DetailedStackStatus.DOWNSCALE_COMPLETED;
        }
        return stackStatus;
    }

    protected DetailedStackStatus getFailedStatus(Map<Object, Object> variables) {
        DetailedStackStatus stackStatus;
        if (isRepair(variables)) {
            stackStatus = DetailedStackStatus.REPAIR_FAILED;
        } else {
            stackStatus = DetailedStackStatus.DOWNSCALE_FAILED;
        }
        return stackStatus;
    }
}
