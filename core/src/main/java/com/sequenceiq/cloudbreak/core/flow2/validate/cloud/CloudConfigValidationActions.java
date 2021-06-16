package com.sequenceiq.cloudbreak.core.flow2.validate.cloud;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.common.api.type.CdpResourceType.fromStackType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.controller.validation.ParametersValidator;
import com.sequenceiq.cloudbreak.controller.validation.datalake.DataLakeValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.ClusterCreationEnvironmentValidator;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackValidator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class CloudConfigValidationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConfigValidationActions.class);

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private StackViewService stackViewService;

    @Inject
    private ParametersValidator parametersValidator;

    @Inject
    private StackValidator stackValidator;

    @Inject
    private TemplateValidator templateValidator;

    @Inject
    private ClusterCreationEnvironmentValidator environmentValidator;

    @Inject
    private DataLakeValidator dataLakeValidator;

    @Inject
    private MultiAzValidator multiAzValidator;

    @Inject
    private UserService userService;

    @Bean(name = "VALIDATE_CLOUD_CONFIG_STATE")
    public Action<?, ?> cloudConfigValidationAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                String name = stack.getName();

                DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
                Credential credential = credentialConverter.convert(environment.getCredential());
                CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);

                ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

                stackValidator.validate(stack, validationBuilder);
                Optional<User> user = userService.getByUserIdAndTenantId(
                        stack.getCreator().getUserId(),
                        stack.getCreator().getTenant().getId());

                Set<InstanceGroup> instanceGroups = context.getStack().getInstanceGroups();
                measure(() -> {
                    for (InstanceGroup instanceGroup : instanceGroups) {
                        LOGGER.info("Validate template for {} name with {} instanceGroup.", name, instanceGroup.toString());
                        StackType type = stack.getType();
                        templateValidator.validate(
                                credential,
                                instanceGroup,
                                stack,
                                fromStackType(type == null ? null : type.name()),
                                user,
                                validationBuilder);

                    }
                }, LOGGER, "Stack's instance templates have been validated in {} ms for stack {}", name);
                multiAzValidator.validateMultiAzForStack(stack.getPlatformVariant(), instanceGroups, validationBuilder);

                ParametersValidationRequest parametersValidationRequest = parametersValidator.validate(
                        stack.getCloudPlatform(),
                        cloudCredential,
                        stack.getParameters(),
                        stack.getCreator().getUserId(),
                        stack.getWorkspace().getId(),
                        validationBuilder);
                parametersValidator.waitResult(parametersValidationRequest, validationBuilder);

                if (!StackType.LEGACY.equals(stack.getType())) {
                    dataLakeValidator.validate(stack, validationBuilder);
                }

                environmentValidator.validate(
                        stack,
                        environment,
                        stack.getCreator(),
                        stack.getType().equals(StackType.WORKLOAD),
                        validationBuilder);

                ValidationResult validationResult = validationBuilder.build();
                if (validationResult.getState() == ValidationResult.State.ERROR || validationResult.hasError()) {
                    LOGGER.debug("Stack request has validation error(s): {}.", validationResult.getFormattedErrors());
                    StackFailureEvent failureEvent = new StackFailureEvent(CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FAILED_EVENT.selector(),
                            payload.getResourceId(),
                            new IllegalStateException(validationResult.getFormattedErrors()));
                    sendEvent(context,
                            CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FAILED_EVENT.selector(),
                            failureEvent);

                } else {
                    LOGGER.debug("Stack validation has been finished without any error.");
                    sendEvent(context, CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FINISHED_EVENT.selector(), payload);
                }
            }

            @Override
            protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new StackFailureEvent(CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FAILED_EVENT.selector(), payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "VALIDATE_CLOUD_CONFIG_FAILED_STATE")
    public Action<?, ?> cloudConfigValidationFailureAction() {
        return new AbstractStackFailureAction<CloudConfigValidationState, CloudConfigValidationEvent>() {

            @Override
            protected StackFailureContext createFlowContext(FlowParameters flowParameters,
                    StateContext<CloudConfigValidationState, CloudConfigValidationEvent> stateContext, StackFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                StackView stackView = stackViewService.findById(payload.getResourceId()).get();
                MDCBuilder.buildMdcContext(stackView);
                flow.setFlowFailed(payload.getException());
                return new StackFailureContext(flowParameters, stackView);
            }

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                String statusReason = payload.getException().getMessage();
                stackUpdaterService.updateStatusAndSendEventWithArgs(context.getStackView().getId(), DetailedStackStatus.PROVISION_FAILED,
                        ResourceEvent.CLOUD_CONFIG_VALIDATION_FAILED, statusReason, statusReason);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FAILURE_HANDLED_EVENT.selector(), context.getStackView().getId());
            }
        };
    }
}
