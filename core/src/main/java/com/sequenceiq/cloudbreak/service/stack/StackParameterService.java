package com.sequenceiq.cloudbreak.service.stack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialSourceRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

import reactor.bus.EventBus;

@Service
public class StackParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackParameterService.class);

    @Inject
    private CredentialService credentialService;

    @Resource
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    public List<StackParamValidation> getStackParams(IdentityUser user, String name, StackRequest stackRequest) {
        List<StackParamValidation> stackParamValidations = new ArrayList<>();
        if (stackRequest.getCredentialSource() != null) {
            stackParamValidations.addAll(getStackParams(user, name, stackRequest.getCredentialSource()));
        } else if (stackRequest.getCredentialId() != null) {
            stackParamValidations.addAll(getStackParams(name, stackRequest.getCredentialId()));
        } else if (stackRequest.getCredentialName() != null) {
            stackParamValidations.addAll(getStackParams(user, name, stackRequest.getCredentialName()));
        } else if (stackRequest.getCredential() != null) {
            stackParamValidations.addAll(getStackParams(name, stackRequest.getCredential()));
        }
        return stackParamValidations;
    }

    private List<StackParamValidation> getStackParams(IdentityUser user, String name, CredentialSourceRequest credentialSourceRequest) {
        LOGGER.debug("Get stack params");
        Credential credential = null;
        if (credentialSourceRequest != null) {
            if (!Strings.isNullOrEmpty(credentialSourceRequest.getSourceName())) {
                credential = credentialService.get(credentialSourceRequest.getSourceName(), user.getAccount());
            } else {
                credential = credentialService.get(credentialSourceRequest.getSourceId());
            }
        }
        return getStackParams(name, credential);
    }

    private List<StackParamValidation> getStackParams(String name, Long credentialId) {
        LOGGER.debug("Get stack params");
        Credential credential = null;
        if (credentialId != null) {
            credential = credentialService.get(credentialId);
        }
        return getStackParams(name, credential);
    }

    private List<StackParamValidation> getStackParams(String name, CredentialRequest credentialRequest) {
        LOGGER.debug("Get stack params");
        Credential credential = null;
        if (credentialRequest != null) {
            credential = conversionService.convert(credentialRequest, Credential.class);
        }
        return getStackParams(name, credential);
    }

    private List<StackParamValidation> getStackParams(IdentityUser user, String name, String credentialName) {
        LOGGER.debug("Get stack params");
        Credential credential = null;
        if (credentialName != null) {
            credential = credentialService.getPublicCredential(credentialName, user);
        }
        return getStackParams(name, credential);
    }

    private List<StackParamValidation> getStackParams(String name, Credential credential) {
        LOGGER.debug("Get stack params");
        if (credential != null) {
            CloudContext cloudContext = new CloudContext(null, name, credential.cloudPlatform(), credential.getOwner());

            GetStackParamValidationRequest getStackParamValidationRequest = new GetStackParamValidationRequest(cloudContext);
            eventBus.notify(getStackParamValidationRequest.selector(), eventFactory.createEvent(getStackParamValidationRequest));
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
