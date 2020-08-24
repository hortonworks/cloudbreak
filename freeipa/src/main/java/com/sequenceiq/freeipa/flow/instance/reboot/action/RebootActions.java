package com.sequenceiq.freeipa.flow.instance.reboot.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure.RebootInstancesResultToCleanupFailureEventConverter;
import com.sequenceiq.freeipa.flow.instance.InstanceEvent;
import com.sequenceiq.freeipa.flow.instance.InstanceFailureEvent;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootContext;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootInstanceEvent;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootService;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootState;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Configuration
public class RebootActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(RebootActions.class);

    @Inject
    private RebootService rebootService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Bean(name = "REBOOT_STATE")
    public Action<?, ?> rebootAction() {
        return new AbstractRebootAction<>(RebootInstanceEvent.class) {
            @Override
            protected void doExecute(RebootContext context, RebootInstanceEvent payload, Map<Object, Object> variables) {
                setOperationId(variables, payload.getOperationId());
                LOGGER.info("Starting reboot for {}", context.getInstanceIds());
                rebootService.startInstanceReboot(context);
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(RebootInstanceEvent payload, Optional<RebootContext> flowContext, Exception ex) {
                return new InstanceFailureEvent(payload.getResourceId(), ex, payload.getInstanceIds());
            }

            @Override
            protected Selectable createRequest(RebootContext context) {
                List<CloudInstance> cloudInstances = context.getInstanceMetaDataList().stream().map(instanceMetaData ->
                        conversionService.convert(instanceMetaData, CloudInstance.class)).collect(Collectors.toList());
                return new RebootInstancesRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudInstances);
            }

            @Override
            protected RebootContext createFlowContext(FlowParameters flowParameters, StateContext<RebootState, RebootEvent> stateContext,
                    RebootInstanceEvent payload) {
                Long stackId = payload.getResourceId();
                Stack stack = stackService.getStackById(stackId);
                List<InstanceMetaData> instances = instanceMetaDataService.findNotTerminatedForStack(stackId).stream()
                        .filter(instanceMetaData -> payload.getInstanceIds().contains(instanceMetaData.getInstanceId())).collect(Collectors.toList());
                MDCBuilder.buildMdcContext(stack);

                Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
                CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.getCloudPlatform(), stack.getCloudPlatform(),
                        location, stack.getOwner(), stack.getAccountId());
                Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());
                CloudCredential cloudCredential = credentialConverter.convert(credential);
                return new RebootContext(flowParameters, stack, instances, cloudContext, cloudCredential);
            }
        };
    }

    @Bean(name = "REBOOT_FINISHED_STATE")
    public Action<?, ?> rebootFinishedAction() {
        return new AbstractRebootAction<>(RebootInstancesResult.class) {
            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(RebootContext context, RebootInstancesResult payload, Map<Object, Object> variables) {
                addMdcOperationId(variables);
                rebootService.finishInstanceReboot(context, payload);
                LOGGER.info("Finished rebooting {}.", context.getInstanceIds());
                Stack stack = context.getStack();
                SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                successDetails.getAdditionalDetails().put("InstanceIds", context.getInstanceIdList());
                operationService.completeOperation(stack.getAccountId(), getOperationId(variables), List.of(successDetails), Collections.emptyList());
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(RebootInstancesResult payload, Optional<RebootContext> flowContext, Exception ex) {
                return new InstanceFailureEvent(payload.getResourceId(), ex, payload.getInstanceIds());
            }

            @Override
            protected Selectable createRequest(RebootContext context) {
                return new InstanceEvent(RebootEvent.REBOOT_FINALIZED_EVENT.event(), context.getStack().getId(), context.getInstanceIdList());
            }

            @Override
            protected RebootContext createFlowContext(FlowParameters flowParameters, StateContext<RebootState,
                    RebootEvent> stateContext, RebootInstancesResult payload) {
                List<InstanceMetaData> instances = payload.getResults().getResults().stream().map(instance -> {
                    InstanceMetaData md = new InstanceMetaData();
                    md.setInstanceId(instance.getCloudInstance().getInstanceId());
                    return md;
                }).collect(Collectors.toList());
                Long stackId = payload.getResourceId();
                Stack stack = stackService.getStackById(stackId);
                MDCBuilder.buildMdcContext(stack);
                return new RebootContext(flowParameters, stack, instances, null, null);
            }
        };
    }

    @Bean(name = "REBOOT_FAILED_STATE")
    public Action<?, ?> rebootFailureAction() {
        return new AbstractRebootAction<>(InstanceFailureEvent.class) {
            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(RebootContext context, InstanceFailureEvent payload, Map<Object, Object> variables) {
                addMdcOperationId(variables);
                rebootService.handleInstanceRebootError(context);
                String message = String.format("Rebooting failed for {}.", context.getInstanceIds());
                LOGGER.error(message);
                Stack stack = context.getStack();
                SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                FailureDetails failureDetails = new FailureDetails(stack.getEnvironmentCrn(), message);
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(successDetails), List.of(failureDetails));
                sendEvent(context, new InstanceEvent(RebootEvent.REBOOT_FAIL_HANDLED_EVENT.event(), context.getStack().getId(), context.getInstanceIdList()));
            }

            @Override
            protected RebootContext createFlowContext(FlowParameters flowParameters, StateContext<RebootState,
                    RebootEvent> stateContext, InstanceFailureEvent payload) {
                Long stackId = payload.getResourceId();
                Stack stack = stackService.getStackById(stackId);
                MDCBuilder.buildMdcContext(stack);

                return new RebootContext(flowParameters, stack, payload.getInstanceIds().stream().map(instanceId -> {
                    InstanceMetaData md = new InstanceMetaData();
                    md.setInstanceId(instanceId);
                    return md;
                }).collect(Collectors.toList()), null, null);
            }

            @Override
            protected Object getFailurePayload(InstanceFailureEvent payload, Optional<RebootContext> flowContext, Exception ex) {
                return new InstanceFailureEvent(payload.getResourceId(), ex, payload.getInstanceIds());
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<InstanceFailureEvent>> payloadConverters) {
                payloadConverters.add(new RebootInstancesResultToCleanupFailureEventConverter());
            }
        };
    }

}
