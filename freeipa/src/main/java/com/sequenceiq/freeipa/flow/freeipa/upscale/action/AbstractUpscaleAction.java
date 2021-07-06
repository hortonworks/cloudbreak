package com.sequenceiq.freeipa.flow.freeipa.upscale.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.AbstractCommonChainAction;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleState;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractUpscaleAction<P extends Payload> extends AbstractCommonChainAction<UpscaleState, UpscaleFlowEvent, StackContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUpscaleAction.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    protected AbstractUpscaleAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<UpscaleState, UpscaleFlowEvent> stateContext,
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
                .withVariant(stack.getCloudPlatform())
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
        return new StackFailureEvent(payload.getResourceId(), ex);
    }

    protected DetailedStackStatus getInProgressStatus(Map<Object, Object> variables) {
        DetailedStackStatus stackStatus;
        if (isRepair(variables)) {
            stackStatus = DetailedStackStatus.REPAIR_IN_PROGRESS;
        } else {
            stackStatus = DetailedStackStatus.UPSCALE_IN_PROGRESS;
        }
        return stackStatus;
    }

    protected DetailedStackStatus getUpscaleCompleteStatus(Map<Object, Object> variables) {
        DetailedStackStatus stackStatus;
        if (isRepair(variables)) {
            stackStatus = DetailedStackStatus.REPAIR_IN_PROGRESS;
        } else {
            stackStatus = DetailedStackStatus.UPSCALE_COMPLETED;
        }
        return stackStatus;
    }

    protected DetailedStackStatus getFailedStatus(Map<Object, Object> variables) {
        DetailedStackStatus stackStatus;
        if (isRepair(variables)) {
            stackStatus = DetailedStackStatus.REPAIR_FAILED;
        } else {
            stackStatus = DetailedStackStatus.UPSCALE_FAILED;
        }
        return stackStatus;
    }
}
