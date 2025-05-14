package com.sequenceiq.cloudbreak.controller.validation;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@Component
public class ParametersValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersValidator.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    public ParametersValidationRequest validate(String platform, String variant, CloudCredential cloudCredential, Map<String, String> parameters,
            Long workspaceId) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        LOGGER.info("Validate the parameters: {}", parameters);
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(platform)
                .withVariant(variant)
                .withWorkspaceId(workspaceId)
                .build();
        ParametersValidationRequest request = new ParametersValidationRequest(cloudCredential, cloudContext, parameters);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        return request;
    }

    public void waitResult(ParametersValidationRequest parametersValidationRequest, ValidationResult.ValidationResultBuilder validationBuilder) {
        if (parametersValidationRequest != null) {
            try {
                ParametersValidationResult result = parametersValidationRequest.await();
                LOGGER.info("Parameter validation result: {}", result);
                Exception exception = result.getErrorDetails();
                if (exception != null) {
                    validationBuilder.error(result.getStatusReason());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Error while sending the parameters validation request", e);
                validationBuilder.error(e.getMessage());
            }
        }
    }
}
