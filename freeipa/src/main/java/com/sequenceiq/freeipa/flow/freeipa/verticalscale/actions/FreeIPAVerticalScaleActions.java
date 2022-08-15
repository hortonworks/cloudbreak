package com.sequenceiq.freeipa.flow.freeipa.verticalscale.actions;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent.STACK_VERTICALSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent.STACK_VERTICALSCALE_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent.STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIPAVerticalScaleService;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIPAVerticalScaleState;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleRequest;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleResult;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScalingTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

import reactor.bus.EventBus;

@Configuration
public class FreeIPAVerticalScaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPAVerticalScaleActions.class);

    @Inject
    private FreeIPAVerticalScaleService freeIPAVerticalScaleService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackService stackService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private EventBus eventBus;

    @Bean(name = "STACK_VERTICALSCALE_STATE")
    public Action<?, ?> stackVerticalScale() {
        return new AbstractFreeIPAVerticalScaleAction<>(FreeIPAVerticalScalingTriggerEvent.class) {
            @Override
            protected void doExecute(StackContext ctx, FreeIPAVerticalScalingTriggerEvent payload, Map<Object, Object> variables) {
                com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.FreeIPAVerticalScaleRequest freeIPAVerticalScaleRequest = payload.getRequest();
                Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
                try {
                    List<Resource> resources = resourceService.findAllByStackId(payload.getResourceId());
                    List<CloudResource> cloudResources =
                            resources.stream().map(resource -> cloudResourceConverter.convert(resource)).collect(Collectors.toList());
                    CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
                    CloudStack cloudStack = cloudStackConverter.convert(stack);
                    cloudStack = cloudStackConverter.updateWithVerticalScaleRequest(cloudStack, freeIPAVerticalScaleRequest);
                    Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
                    CloudContext cloudContext = CloudContext.Builder.builder()
                            .withId(stack.getId())
                            .withName(stack.getName())
                            .withCrn(stack.getResourceCrn())
                            .withPlatform(stack.getCloudPlatform())
                            .withVariant(stack.getPlatformvariant())
                            .withLocation(location)
                            .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                            .build();

                    FreeIPAVerticalScaleRequest request = new FreeIPAVerticalScaleRequest(
                            cloudContext,
                            cloudCredential,
                            cloudStack,
                            cloudResources,
                            freeIPAVerticalScaleRequest);

                    sendEvent(ctx, request);
                } catch (Exception e) {
                    LOGGER.error("Failed to Vertical scaling FreeIPA", e);
                    sendEvent(ctx, STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT.selector(),
                            new UpscaleFailureEvent(stack.getId(), "Vertical Scale update", Set.of(), Map.of(), e));
                }
            }
        };
    }

    @Bean(name = "STACK_VERTICALSCALE_FINISHED_STATE")
    public Action<?, ?> verticalScaleFinished() {
        return new AbstractFreeIPAVerticalScaleAction<>(FreeIPAVerticalScaleResult.class) {

            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(StackContext context, FreeIPAVerticalScaleResult payload, Map<Object, Object> variables) {
                freeIPAVerticalScaleService.updateTemplateWithVerticalScaleInformation(context.getStack().getId(), payload.getFreeIPAVerticalScaleV1Request());
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.STOPPED, "Vertical scale complete");
                sendEvent(context, STACK_VERTICALSCALE_FINALIZED_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "STACK_VERTICALSCALE_FAILED_STATE")
    public Action<?, ?> verticalScaleFailedAction() {
        return new AbstractFreeIPAVerticalScaleAction<>(FreeIPAVerticalScaleFailureEvent.class) {
            @Inject
            private OperationService operationService;

            @Override
            protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIPAVerticalScaleState,
                    FreeIPAVerticalScaleEvent> stateContext,
                    FreeIPAVerticalScaleFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(StackContext context, FreeIPAVerticalScaleFailureEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                String environmentCrn = stack.getEnvironmentCrn();
                SuccessDetails successDetails = new SuccessDetails(environmentCrn);
                successDetails.getAdditionalDetails()
                        .put(payload.getFailedPhase(), payload.getSuccess() == null ? List.of() : new ArrayList<>(payload.getSuccess()));
                String message = "Vertical scale failed during " + payload.getFailedPhase();
                FailureDetails failureDetails = new FailureDetails(environmentCrn, message);
                if (payload.getFailureDetails() != null) {
                    failureDetails.getAdditionalDetails().putAll(payload.getFailureDetails());
                }
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.VERTICAL_SCALE_FAILED, errorReason);
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(successDetails), List.of(failureDetails));
                enableStatusChecker(stack, "Failed vertical scaling FreeIPA");
                enableNodeStatusChecker(stack, "Failed vertical scaling FreeIPA");
                sendEvent(context, STACK_VERTICALSCALE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
