package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import reactor.bus.Event;
import reactor.bus.EventBus;

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
        LOGGER.info("Received event: {}", event);
        ValidationRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        ValidationResult result;
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            CloudStack cloudStack = request.getCloudStack();
            for (Validator v : connector.validators()) {
                v.validate(cloudStack);
            }
            result = new ValidationResult(request);
        } catch (Exception e) {
            result = new ValidationResult(e, request);
        }
        request.getResult().onNext(result);
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }
}
