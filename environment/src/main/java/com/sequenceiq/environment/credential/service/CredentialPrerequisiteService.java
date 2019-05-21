package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.environment.TempConstants.TEMP_EXTERNAL_ID;
import static com.sequenceiq.environment.TempConstants.TEMP_USER_ID;
import static com.sequenceiq.environment.TempConstants.TEMP_WORKSPACE_ID;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Service
public class CredentialPrerequisiteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialPrerequisiteService.class);

    private static final String ROLE_ARN_PARAMTER_KEY = "roleArn";

    private static final String EXTERNAL_ID_PARAMETER_KEY = "externalId";

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    public CredentialPrerequisitesResponse getPrerequisites(String cloudPlatform, String deploymentAddress) {
        CloudContext cloudContext = new CloudContext(null, null, cloudPlatform, TEMP_USER_ID, TEMP_WORKSPACE_ID);
        CredentialPrerequisitesRequest request = new CredentialPrerequisitesRequest(cloudContext, TEMP_EXTERNAL_ID, deploymentAddress);
        LOGGER.debug("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        String message = String.format("Failed to get prerequisites for platform '%s': ", cloudPlatform);
        try {
            CredentialPrerequisitesResult res = request.await();
            LOGGER.debug("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.info(message, res.getErrorDetails());
                throw new BadRequestException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            return res.getCredentialPrerequisitesResponse();
        } catch (InterruptedException e) {
            LOGGER.error(message, e);
            throw new OperationException(e);
        }
    }

    public Credential decorateCredential(Credential credential) {
        String attributes = credential.getAttributes();
        Map<String, Object> newAttributes = convertJsonStringToMap(attributes);
        boolean attributesChanged = false;
        if (StringUtils.isNoneEmpty((String) newAttributes.get(ROLE_ARN_PARAMTER_KEY))) {
            newAttributes.put(EXTERNAL_ID_PARAMETER_KEY, TEMP_EXTERNAL_ID);
            attributesChanged = true;
        }
        if (attributesChanged) {
            saveNewAttributesToCredential(credential, newAttributes);
        }
        return credential;
    }

    private Map<String, Object> convertJsonStringToMap(String attributes) {
        return isEmpty(attributes) ? new HashMap<>() : new Json(attributes).getMap();
    }

    private void saveNewAttributesToCredential(Credential credential, Map<String, Object> newAttributes) {
        credential.setAttributes(new Json(newAttributes).getValue());
    }
}
