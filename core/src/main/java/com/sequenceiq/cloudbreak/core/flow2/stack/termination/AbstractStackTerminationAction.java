package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.core.FlowParameters;

abstract class AbstractStackTerminationAction<P extends Payload>
        extends AbstractStackAction<StackTerminationState, StackTerminationEvent, StackTerminationContext, P> {

    public static final String TERMINATION_TYPE = "TERMINATION_TYPE";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private StackUtil stackUtil;

    protected AbstractStackTerminationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackTerminationContext createFlowContext(FlowParameters flowParameters,
                                                        StateContext<StackTerminationState, StackTerminationEvent> stateContext, P payload) {
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        TerminationType terminationType = (TerminationType) variables.getOrDefault(TERMINATION_TYPE, TerminationType.REGULAR);
        StackDto stack = stackDtoService.getById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withOriginalName(stack.getOriginalName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspaceId())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .withTenantId(stack.getTenant().getId())
                .build();
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        List<CloudResource> resources = resourceService.getAllByStackId(stack.getId()).stream()
                .map(r -> cloudResourceConverter.convert(r))
                .collect(Collectors.toList());
        return createStackTerminationContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack, resources, terminationType);
    }

    protected StackTerminationContext createStackTerminationContext(FlowParameters flowParameters, StackDto stack, CloudContext cloudContext,
        CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> resources, TerminationType terminationType) {
        return new StackTerminationContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack, resources, terminationType);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackTerminationContext> flowContext, Exception ex) {
        return new StackTerminationFailureEvent(payload.getResourceId(), ex);
    }
}
