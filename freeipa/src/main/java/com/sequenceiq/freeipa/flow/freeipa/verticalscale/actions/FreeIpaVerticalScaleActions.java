package com.sequenceiq.freeipa.flow.freeipa.verticalscale.actions;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.VERTICAL_SCALE_FAILED;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleService;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleRequest;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleResult;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScalingTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class FreeIpaVerticalScaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaVerticalScaleActions.class);

    @Inject
    private FreeIpaVerticalScaleService freeIPAVerticalScaleService;

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

    @Bean(name = "STACK_VERTICALSCALE_STATE")
    public Action<?, ?> stackVerticalScale() {
        return new AbstractFreeIpaVerticalScaleAction<>(FreeIpaVerticalScalingTriggerEvent.class) {
            @Override
            protected void doExecute(StackContext ctx, FreeIpaVerticalScalingTriggerEvent payload, Map<Object, Object> variables) {
                VerticalScaleRequest freeIPAVerticalScaleRequest = payload.getRequest();
                Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
                try {
                    List<Resource> resources = resourceService.findAllByStackId(payload.getResourceId());
                    List<CloudResource> cloudResources =
                            resources.stream().map(resource -> cloudResourceConverter.convert(resource)).collect(Collectors.toList());
                    CloudCredential cloudCredential = ctx.getCloudCredential();
                    CloudStack cloudStack = cloudStackConverter.convert(stack);
                    cloudStack = cloudStackConverter.updateWithVerticalScaleRequest(cloudStack, freeIPAVerticalScaleRequest);
                    LOGGER.debug("The changed cloudstack is {}", cloudStack);
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

                    FreeIpaVerticalScaleRequest request = new FreeIpaVerticalScaleRequest(
                            cloudContext,
                            cloudCredential,
                            cloudStack,
                            cloudResources,
                            freeIPAVerticalScaleRequest);

                    sendEvent(ctx, request);
                } catch (Exception e) {
                    LOGGER.error("Failed to Vertical scaling FreeIPA", e);
                    sendEvent(ctx, STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT.selector(),
                            new FreeIpaVerticalScaleFailureEvent(stack.getId(), "Vertical Scale update",
                                    Set.of(), ERROR, Map.of(), e)
                    );
                }
            }
        };
    }

    @Bean(name = "STACK_VERTICALSCALE_FINISHED_STATE")
    public Action<?, ?> verticalScaleFinished() {
        return new AbstractFreeIpaVerticalScaleAction<>(FreeIpaVerticalScaleResult.class) {

            @Override
            protected void doExecute(StackContext context, FreeIpaVerticalScaleResult payload, Map<Object, Object> variables) {
                Long stackId = context.getStack().getId();
                freeIPAVerticalScaleService.updateTemplateWithVerticalScaleInformation(stackId, payload.getFreeIPAVerticalScaleRequest());
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.STOPPED, "Vertical scale complete");
                sendEvent(context, FINALIZED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "STACK_VERTICALSCALE_FAILED_STATE")
    public Action<?, ?> verticalScaleFailedAction() {
        return new AbstractFreeIpaVerticalScaleAction<>(FreeIpaVerticalScaleFailureEvent.class) {

            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(StackContext context, FreeIpaVerticalScaleFailureEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                String errorReason = getErrorReason(payload.getException());
                String message = "Vertical scale failed with [" + payload.getFailedPhase() + "]. " + errorReason;
                LOGGER.debug(message);

                stackUpdater.updateStackStatus(stack.getId(), VERTICAL_SCALE_FAILED, message);
                if (isOperationIdSet(variables)) {
                    operationService.failOperation(stack.getAccountId(), getOperationId(variables), message);
                }
                enableStatusChecker(stack, "Failed vertical scaling FreeIPA");
                sendEvent(context, STACK_VERTICALSCALE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}