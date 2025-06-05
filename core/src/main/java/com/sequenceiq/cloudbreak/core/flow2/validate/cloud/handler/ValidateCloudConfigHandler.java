package com.sequenceiq.cloudbreak.core.flow2.validate.cloud.handler;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.common.api.type.CdpResourceType.fromStackType;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.controller.validation.ParametersValidator;
import com.sequenceiq.cloudbreak.controller.validation.datalake.DataLakeValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.ClusterCreationEnvironmentValidator;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.StackValidator;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateValidatorAndUpdater;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.event.ValidateCloudConfigRequest;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ValidateCloudConfigHandler extends ExceptionCatcherEventHandler<ValidateCloudConfigRequest> {

    private static final Logger LOGGER = getLogger(ValidateCloudConfigHandler.class);

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private StackService stackService;

    @Inject
    private ParametersValidator parametersValidator;

    @Inject
    private StackValidator stackValidator;

    @Inject
    private TemplateValidatorAndUpdater templateValidatorAndUpdater;

    @Inject
    private ClusterCreationEnvironmentValidator environmentValidator;

    @Inject
    private DataLakeValidator dataLakeValidator;

    @Inject
    private MultiAzValidator multiAzValidator;

    @Override
    protected Selectable doAccept(HandlerEvent<ValidateCloudConfigRequest> event) {
        ValidateCloudConfigRequest data = event.getData();
        Stack stack = stackService.getByIdWithLists(data.getResourceId());
        String name = stack.getName();

        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        Credential credential = credentialConverter.convert(environment.getCredential());
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);

        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        stackValidator.validate(stack, validationBuilder);

        Set<InstanceGroup> instanceGroups = stack.getInstanceGroups();
        measure(() -> {
            for (InstanceGroup instanceGroup : instanceGroups) {
                LOGGER.info("Validate template for {} name with {} instanceGroup.", name, instanceGroup.toString());
                StackType type = stack.getType();
                templateValidatorAndUpdater.validate(
                        environment,
                        credential,
                        instanceGroup,
                        stack,
                        fromStackType(type == null ? null : type.name()),
                        validationBuilder);

            }
        }, LOGGER, "Stack's instance templates have been validated in {} ms for stack {}", name);

        multiAzValidator.validateMultiAzForStack(stack, validationBuilder);

        ParametersValidationRequest parametersValidationRequest = parametersValidator.validate(
                stack.getCloudPlatform(),
                stack.getPlatformVariant(),
                cloudCredential,
                stack.getParameters(),
                stack.getWorkspaceId());
        parametersValidator.waitResult(parametersValidationRequest, validationBuilder);

        if (!StackType.LEGACY.equals(stack.getType())) {
            dataLakeValidator.validate(stack, validationBuilder);
        }

        environmentValidator.validate(
                stack,
                environment,
                stack.getType().equals(StackType.WORKLOAD),
                validationBuilder);

        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.getState() == ValidationResult.State.ERROR || validationResult.hasError()) {
            LOGGER.debug("Stack request has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new IllegalStateException(validationResult.getFormattedErrors());

        } else {
            LOGGER.debug("Stack validation has been finished without any error.");
            return new StackEvent(CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FINISHED_EVENT.selector(), data.getResourceId());
        }
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateCloudConfigRequest> event) {
        return new StackFailureEvent(CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FAILED_EVENT.selector(), resourceId, e);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateCloudConfigRequest.class);
    }
}
