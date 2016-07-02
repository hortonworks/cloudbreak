package com.sequenceiq.cloudbreak.service.stack;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class StackParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackParameterService.class);

    @Inject
    private CredentialService credentialService;

    @Inject
    private EventBus eventBus;

    public List<StackParamValidation> getStackParams(StackRequest stackRequest) {
        LOGGER.debug("Get stack params");
        Long credentialId = stackRequest.getCredentialId();
        if (credentialId != null) {
            Credential credential = credentialService.get(credentialId);
            CloudContext cloudContext = new CloudContext(credential.getId(), stackRequest.getName(), credential.cloudPlatform(), credential.getOwner());

            GetStackParamValidationRequest getStackParamValidationRequest = new GetStackParamValidationRequest(cloudContext);
            eventBus.notify(getStackParamValidationRequest.selector(), Event.wrap(getStackParamValidationRequest));
            try {
                GetStackParamValidationResult res = getStackParamValidationRequest.await();
                LOGGER.info("Get stack params result: {}", res);
                if (res.getStatus().equals(EventStatus.FAILED)) {
                    LOGGER.error("Failed to get stack params", res.getErrorDetails());
                    throw new OperationException(res.getErrorDetails());
                }
                return res.getStackParamValidations();
            } catch (InterruptedException e) {
                LOGGER.error("Error while getting the stack params", e);
                throw new OperationException(e);
            }
        } else {
            return Collections.emptyList();
        }
    }
}
