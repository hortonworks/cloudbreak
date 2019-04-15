package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

abstract class AbstractStackUpscaleAction<P extends Payload> extends AbstractStackAction<StackUpscaleState, StackUpscaleEvent, StackScalingFlowContext, P> {
    static final String INSTANCEGROUPNAME = "INSTANCEGROUPNAME";

    static final String ADJUSTMENT = "ADJUSTMENT";

    static final String UPSCALE_CANDIDATE_ADDRESSES = "UPSCALE_CANDIDATE_ADDRESSES";

    static final String HOSTNAMES = "HOSTNAMES";

    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    AbstractStackUpscaleAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackScalingFlowContext createFlowContext(String flowId, StateContext<StackUpscaleState, StackUpscaleEvent> stateContext, P payload) {
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getPlatformVariant(),
                location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackScalingFlowContext(flowId, stack, cloudContext, cloudCredential, cloudStack, getInstanceGroupName(variables), Collections.emptySet(),
                getAdjustment(variables), getHostNames(variables));
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackScalingFlowContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getStackId(), ex);
    }

    private String getInstanceGroupName(Map<Object, Object> variables) {
        return (String) variables.get(INSTANCEGROUPNAME);
    }

    private Integer getAdjustment(Map<Object, Object> variables) {
        return (Integer) variables.get(ADJUSTMENT);
    }

    private Set<String> getHostNames(Map<Object, Object> variables) {
        return (Set<String>) variables.get(HOSTNAMES);
    }
}
