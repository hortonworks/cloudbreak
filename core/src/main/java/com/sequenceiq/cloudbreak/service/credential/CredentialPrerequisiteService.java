package com.sequenceiq.cloudbreak.service.credential;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.v3.credential.CredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserPreferences;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.user.UserPreferencesService;

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

    @Inject
    private UserPreferencesService userPreferencesService;

    public CredentialPrerequisites getPrerequisites(User user, Workspace workspace, String cloudPlatform) {
        CloudContext cloudContext = new CloudContext(null, null, cloudPlatform,
                user.getUserId(), user.getUserId(), workspace.getId());
        UserPreferences userPreferences = userPreferencesService.getWithExternalId(user);
        CredentialPrerequisitesRequest request = new CredentialPrerequisitesRequest(cloudContext, userPreferences.getExternalId());
        LOGGER.info("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        String message = String.format("Failed to get prerequisites for platform '%s': ", cloudPlatform);
        try {
            CredentialPrerequisitesResult res = request.await();
            LOGGER.info("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.error(message, res.getErrorDetails());
                throw new BadRequestException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            return res.getCredentialPrerequisites();
        } catch (InterruptedException e) {
            LOGGER.error(message, e);
            throw new OperationException(e);
        }
    }

    public Credential decorateCredential(Credential credential, String userId) {
        String attributes = credential.getAttributes();
        Map<String, Object> newAttributes = isEmpty(attributes) ? new HashMap<>() : new Json(attributes).getMap();
        if (StringUtils.isNoneEmpty(String.valueOf(newAttributes.get(ROLE_ARN_PARAMTER_KEY)))) {
            Optional<UserPreferences> userPreferencesOptional = userPreferencesService.getByUserId(userId);
            if (userPreferencesOptional.isPresent() && StringUtils.isNoneEmpty(userPreferencesOptional.get().getExternalId())) {
                String externalId = userPreferencesOptional.get().getExternalId();
                newAttributes.put(EXTERNAL_ID_PARAMETER_KEY, externalId);
                saveNewAttributesToCredential(credential, newAttributes);
            }
        }
        return credential;
    }

    private void saveNewAttributesToCredential(Credential credential, Map<String, Object> newAttributes) {
        try {
            credential.setAttributes(new Json(newAttributes).getValue());
        } catch (IOException ex) {
            LOGGER.error("New prerequisite attributes could not be added to the credential.", ex);
            throw new OperationException(ex);
        }
    }
}
