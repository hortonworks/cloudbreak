package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AWS_RESTRICTED_POLICY;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.environment.TempConstants.TEMP_USER_ID;
import static com.sequenceiq.environment.TempConstants.TEMP_WORKSPACE_ID;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialExperiencePolicyRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialExperiencePolicyResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.exception.CredentialOperationException;
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

    private final EntitlementService entitlementService;

    private ExperienceConnectorService experienceConnectorService;

    public CredentialPrerequisiteService(EventBus eventBus, UserPreferencesService userPreferencesService, ErrorHandlerAwareReactorEventFactory eventFactory,
            ExperienceConnectorService experienceConnectorService, EntitlementService entitlementService) {
        this.experienceConnectorService = experienceConnectorService;
        this.userPreferencesService = userPreferencesService;
        this.entitlementService = entitlementService;
        this.eventFactory = eventFactory;
        this.eventBus = eventBus;
    }

    public CredentialPrerequisitesResponse getPrerequisites(String cloudPlatform, String deploymentAddress, CredentialType type) {
        CredentialPrerequisitesResponse result = getCloudbreakPrerequisites(cloudPlatform, deploymentAddress, type);
        if (isPolicyFetchFromExperiencesAllowed()) {
            if (AWS.name().equalsIgnoreCase(cloudPlatform)) {
                try {
                    Map<String, String> policies = getExperiencePrerequisites(cloudPlatform);
                    if (result.getAws().getPolicies() != null) {
                        policies.putAll(result.getAws().getPolicies());
                    }
                    fillPoliciesWithDefaultIfMissing(result.getAws().getPolicyJson(), policies);
                    result.getAws().setPolicies(policies);
                } catch (Exception e) {
                    LOGGER.warn("Something has happened during the granular policy fetch from the experiences!", e);
                }
            } else {
                LOGGER.info("Fetching is enabled but the requested prerequisites from the experiences are addressed for a currently not supported " +
                        "cloud platform: " + cloudPlatform);
            }
        } else {
            LOGGER.info("Fetching fine graded policies from the experiences has disabled by the entitlement: "
                    + CDP_AWS_RESTRICTED_POLICY.name());
        }
        return result;
    }

    public CredentialPrerequisitesResponse getCloudbreakPrerequisites(String cloudPlatform, String deploymentAddress, CredentialType type) {
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
            return res.getCredentialPrerequisitesResponse();
        } catch (InterruptedException e) {
            LOGGER.error(message, e);
            throw new OperationException(e);
        }
    }

    public Map<String, String> getExperiencePrerequisites(String cloudPlatform) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(cloudPlatform)
                .withUserId(TEMP_USER_ID)
                .withWorkspaceId(TEMP_WORKSPACE_ID)
                .build();
        CredentialExperiencePolicyRequest request = new CredentialExperiencePolicyRequest(cloudContext);
        LOGGER.debug("Triggering event: {}", request);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        String message = String.format("Failed to get experience policies for platform '%s': ", cloudPlatform);
        try {
            CredentialExperiencePolicyResult res = request.await();
            LOGGER.debug("Result: {}", res);
            if (res.getStatus() != EventStatus.OK) {
                LOGGER.warn(message, res.getErrorDetails());
                throw new BadRequestException(message + res.getErrorDetails(), res.getErrorDetails());
            }
            return res.getPolicies();
        } catch (InterruptedException e) {
            LOGGER.error(message, e);
            throw new CredentialOperationException(e);
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

    private void fillPoliciesWithDefaultIfMissing(String defaultAsteriskPolicyJson, Map<String, String> policies) {
        policies.forEach((experienceName, policy) -> {
            if (isEmpty(policy)) {
                policies.put(experienceName, defaultAsteriskPolicyJson);
            }
        });
    }

    private boolean isPolicyFetchFromExperiencesAllowed() {
        return entitlementService.awsRestrictedPolicy(ThreadBasedUserCrnProvider.getAccountId());
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
