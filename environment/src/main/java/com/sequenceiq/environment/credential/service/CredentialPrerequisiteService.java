package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.environment.TempConstants.TEMP_USER_ID;
import static com.sequenceiq.environment.TempConstants.TEMP_WORKSPACE_ID;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialExperiencePolicyRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialExperiencePolicyResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.ExperienceConnectorService;
import com.sequenceiq.environment.user.UserPreferencesService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Service
public class CredentialPrerequisiteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialPrerequisiteService.class);

    private EventBus eventBus;

    private UserPreferencesService userPreferencesService;

    private ErrorHandlerAwareReactorEventFactory eventFactory;

    private ExperienceConnectorService experienceConnectorService;

    public CredentialPrerequisiteService(EventBus eventBus, UserPreferencesService userPreferencesService, ErrorHandlerAwareReactorEventFactory eventFactory,
            ExperienceConnectorService experienceConnectorService) {
        this.experienceConnectorService = experienceConnectorService;
        this.userPreferencesService = userPreferencesService;
        this.eventFactory = eventFactory;
        this.eventBus = eventBus;
    }

    public CredentialPrerequisitesResponse getPrerequisites(String cloudPlatform, String deploymentAddress, CredentialType type) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(cloudPlatform)
                .withUserId(TEMP_USER_ID)
                .withWorkspaceId(TEMP_WORKSPACE_ID)
                .build();
        CredentialPrerequisitesRequest request = new CredentialPrerequisitesRequest(cloudContext,
                userPreferencesService.getExternalIdForCurrentUser(), deploymentAddress, type);
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
            CredentialPrerequisitesResponse response = res.getCredentialPrerequisitesResponse();
            collectAndFillCredentialPrerequisitesBasedOnProvider(cloudPlatform, response);
            return response;
        } catch (InterruptedException e) {
            LOGGER.error(message, e);
            throw new OperationException(e);
        }
    }

    public Map<String, String> getExperiencePolicies(String cloudPlatform) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(cloudPlatform)
                .withUserId(TEMP_USER_ID)
                .withWorkspaceId(TEMP_WORKSPACE_ID)
                .build();
        CredentialExperiencePolicyRequest request = new CredentialExperiencePolicyRequest(cloudContext);
        LOGGER.debug("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        String message = String.format("Failed to get experience cucc for platform '%s': ", cloudPlatform);
        try {
            CredentialExperiencePolicyResult res = request.await();
            LOGGER.debug("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.info(message, res.getErrorDetails());
                throw new BadRequestException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            return res.getPolicies();
        } catch (InterruptedException e) {
            LOGGER.error(message, e);
            throw new OperationException(e);
        }
    }

    public Credential decorateCredential(Credential credential) {
        CredentialAttributes credentialAttributes = getCredentialAttributes(credential);
        if (isRoleArnSet(credentialAttributes)) {
            if (credentialAttributes.getAws().getRoleBased().getExternalId() == null) {
                credentialAttributes.getAws().getRoleBased().setExternalId(userPreferencesService.getExternalIdForCurrentUser());
                saveNewAttributesToCredential(credential, credentialAttributes);
            }
        }
        return credential;
    }

    private void collectAndFillCredentialPrerequisitesBasedOnProvider(String cloudProvider, CredentialPrerequisitesResponse response) {
        EnvironmentExperienceDto dto = new EnvironmentExperienceDto.Builder().withCloudPlatform(cloudProvider)
                .withAccountId(ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN).build();
        Map<String, String> policies = experienceConnectorService.collectExperiencePoliciesForCredentialCreation(dto);
        switch (CloudPlatform.valueOf(cloudProvider)) {
            case AWS: {
                response.getAws().setPolicies(policies);
                break;
            }
            case AZURE: {
                response.getAzure().setPolicies(policies);
                break;
            }
            default: break;
        }
    }

    private boolean isRoleArnSet(CredentialAttributes credentialAttributes) {
        return credentialAttributes != null
                && credentialAttributes.getAws() != null
                && credentialAttributes.getAws().getRoleBased() != null
                && credentialAttributes.getAws().getRoleBased().getRoleArn() != null;
    }

    private CredentialAttributes getCredentialAttributes(Credential credential) {
        try {
            return new Json(credential.getAttributes()).get(CredentialAttributes.class);
        } catch (IOException ignore) {
        }
        return null;
    }

    private void saveNewAttributesToCredential(Credential credential, CredentialAttributes credentialAttributes) {
        credential.setAttributes(new Json(credentialAttributes).getValue());
    }
}
