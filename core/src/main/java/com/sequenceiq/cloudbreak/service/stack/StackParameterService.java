package com.sequenceiq.cloudbreak.service.stack;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.OperationException;

import reactor.bus.EventBus;

@Service
//TODO: Where we use this?
public class StackParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackParameterService.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    public List<StackParamValidation> getStackParams(String name, Stack stack) {
        LOGGER.debug("Get stack params");
        Credential credential = stack.getCredential();
        if (credential != null) {
            CloudContext cloudContext = new CloudContext(null, name, credential.cloudPlatform(),
                    stack.getCreator().getUserId(), stack.getWorkspace().getId());

            GetStackParamValidationRequest getStackParamValidationRequest = new GetStackParamValidationRequest(cloudContext);
            eventBus.notify(getStackParamValidationRequest.selector(), eventFactory.createEvent(getStackParamValidationRequest));
            try {
                GetStackParamValidationResult res = getStackParamValidationRequest.await();
                LOGGER.debug("Get stack params result: {}", res);
                if (res.getStatus().equals(EventStatus.FAILED)) {
                    LOGGER.info("Failed to get stack params", res.getErrorDetails());
                    throw new OperationException(res.getErrorDetails());
                }
                return res.getStackParamValidations();
            } catch (InterruptedException e) {
                LOGGER.info("Error while getting the stack params", e);
                throw new OperationException(e);
            }
        } else {
            return Collections.emptyList();
        }
    }
}
