package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

abstract class AbstractInstanceTerminationAction<P extends InstancePayload>
        extends AbstractStackAction<InstanceTerminationState, InstanceTerminationEvent, InstanceTerminationContext, P> {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Inject
    private StackUtil stackUtil;

    protected AbstractInstanceTerminationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected InstanceTerminationContext createFlowContext(FlowParameters flowParameters,
        StateContext<InstanceTerminationState, InstanceTerminationEvent> stateContext, P payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspaceId())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .withTenantId(stack.getTenantId())
                .build();
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
        final Set<String> instanceIds = payload.getInstanceIds();
        List<InstanceMetadataView> instanceMetaDataList = instanceMetaDataService.findAllViewByStackIdAndInstanceId(stack.getId(), instanceIds);
        if (instanceMetaDataList.size() != instanceIds.size()) {
            List<String> missingInstanceIds = instanceMetaDataList.stream()
                    .filter(im -> !instanceIds.contains(im.getInstanceId()))
                    .map(im -> im.getInstanceId())
                    .collect(Collectors.toList());
            throw new NotFoundException("Missing instances with instanceIds: " + String.join(", ", missingInstanceIds));
        }
        List<CloudInstance> cloudInstances = new ArrayList<>(metadataConverter.convert(instanceMetaDataList, stack));
        return new InstanceTerminationContext(flowParameters, stack, cloudContext, cloudCredential, cloudInstances,
                instanceMetaDataList);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<InstanceTerminationContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }

}
