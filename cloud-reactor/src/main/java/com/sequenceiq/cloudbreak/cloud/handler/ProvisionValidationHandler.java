package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudPlatformValidationWarningException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@Component
public class ProvisionValidationHandler implements CloudPlatformEventHandler<ValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionValidationHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<ValidationRequest> type() {
        return ValidationRequest.class;
    }

    @Override
    public void accept(Event<ValidationRequest> event) {
        LOGGER.debug("Received event: {}", event);
        ValidationRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        ValidationResult result;
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
            CloudStack cloudStack = request.getCloudStack();
            Set<String> warningMessages = new HashSet<>();
            for (Validator validator : connector.validators(ValidatorType.ALL)) {
                executeValidation(ac, cloudStack, warningMessages, validator);
            }
            result = new ValidationResult(request.getResourceId(), warningMessages);
        } catch (RuntimeException e) {
            result = new ValidationResult(e, request.getResourceId());
        }
        request.getResult().onNext(result);
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private void executeValidation(AuthenticatedContext ac, CloudStack cloudStack, Set<String> warningMessages, Validator v) {
        try {
            v.validate(ac, cloudStack);
        } catch (CloudPlatformValidationWarningException e) {
            LOGGER.warn("{} sent the following warning: {}", v.getClass().getSimpleName(), e.getMessage());
            warningMessages.add(e.getMessage());
        }
    }
}
