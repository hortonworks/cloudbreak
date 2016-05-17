package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;

public abstract class AbstractStackUpscaleAction<P extends Payload> extends AbstractAction<StackUpscaleState, StackUpscaleEvent, StackScalingFlowContext, P> {
    protected static final String INSTANCEGROUPNAME = "INSTANCEGROUPNAME";
    private static final String ADJUSTMENT = "ADJUSTMENT";
    private static final String SCALINGTYPE = "SCALINGTYPE";

    @Inject
    private StackService stackService;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;
    @Inject
    private StackScalingService stackScalingService;

    protected AbstractStackUpscaleAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackScalingFlowContext createFlowContext(String flowId, StateContext<StackUpscaleState, StackUpscaleEvent> stateContext, P payload) {
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        Stack stack = stackService.getById(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        String instanceGroupName = extractInstanceGroupName(payload, variables);
        Integer adjustment = extractAdjustment(payload, variables);
        ScalingType scalingType = extractScalingType(payload, variables);
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackScalingFlowContext(flowId, stack, cloudContext, cloudCredential, cloudStack, instanceGroupName, Collections.<String>emptySet(),
                adjustment, scalingType);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackScalingFlowContext> flowContext, Exception ex) {
        return new FlowFailureEvent(payload.getStackId(), ex);
    }

    private ScalingType extractScalingType(P payload, Map<Object, Object> variables) {
        if (payload instanceof StackScalingContext) {
            StackScalingContext ssc = (StackScalingContext) payload;
            variables.put(SCALINGTYPE, ssc.getScalingType());
            return ssc.getScalingType();
        }
        return getScalingType(variables);
    }

    private Integer extractAdjustment(P payload, Map<Object, Object> variables) {
        if (payload instanceof StackScalingContext) {
            StackScalingContext ssc = (StackScalingContext) payload;
            variables.put(ADJUSTMENT, ssc.getScalingAdjustment());
            return ssc.getScalingAdjustment();
        }
        return getAdjustment(variables);
    }

    private String extractInstanceGroupName(P payload, Map<Object, Object> variables) {
        if (payload instanceof StackScalingContext) {
            StackScalingContext ssc = (StackScalingContext) payload;
            variables.put(INSTANCEGROUPNAME, ssc.getInstanceGroup());
            return ssc.getInstanceGroup();
        }
        return getInstanceGroupName(variables);
    }

    protected String getInstanceGroupName(Map<Object, Object> variables) {
        return (String) variables.get(INSTANCEGROUPNAME);
    }

    protected Integer getAdjustment(Map<Object, Object> variables) {
        return (Integer) variables.get(ADJUSTMENT);
    }

    protected ScalingType getScalingType(Map<Object, Object> variables) {
        return (ScalingType) variables.get(SCALINGTYPE);
    }
}
