package com.sequenceiq.cloudbreak.controller.validation;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

import reactor.bus.EventBus;

@Component
public class ParametersValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersValidator.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    public void validate(String platform, CloudCredential cloudCredential, Map<String, String> parameters, String userId, Long workspaceId) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        LOGGER.debug("Validate the parameters: {}", parameters);
        CloudContext cloudContext = new CloudContext(null, null, platform, null, userId, workspaceId);
        ParametersValidationRequest request = new ParametersValidationRequest(cloudCredential, cloudContext, parameters);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            ParametersValidationResult result = request.await();
            LOGGER.info("Parameter validation result: {}", result);
            Exception exception = result.getErrorDetails();
            if (exception != null) {
                throw new BadRequestException(result.getStatusReason(), exception);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while sending the parameters validation request", e);
            throw new OperationException(e);
        }
    }

}
