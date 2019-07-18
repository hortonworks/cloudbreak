package com.sequenceiq.environment.credential.service;


import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.CodeGrantFlowAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.exception.CredentialOperationException;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.notification.NotificationSender;
import com.sequenceiq.notification.ResourceEvent;

@Service
public class CredentialService extends AbstractCredentialService {

    private static final String DEPLOYMENT_ADDRESS_ATTRIBUTE_NOT_FOUND = "The 'deploymentAddress' parameter needs to be specified in the interactive login "
            + "request!";

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

    private static final String NOT_FOUND_FORMAT_MESSAGE = "Credential with name:";

    @Inject
    private CredentialRepository repository;

    @Inject
    private CredentialValidator credentialValidator;

    @Inject
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Inject
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @Inject
    private SecretService secretService;

    protected CredentialService(NotificationSender notificationSender, CloudbreakMessagesService messagesService,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms) {
        super(notificationSender, messagesService, enabledPlatforms);
    }

    public Set<Credential> listAvailablesByAccountId(String accountId) {
        return repository.findAllByAccountId(accountId, getEnabledPlatforms());
    }

    public Credential getByNameForAccountId(String name, String accountId) {
        return repository.findByNameAndAccountId(name, accountId, getEnabledPlatforms()).orElseThrow(notFound(NOT_FOUND_FORMAT_MESSAGE, name));
    }

    public Credential getByCrnForAccountId(String crn, String accountId) {
        return repository.findByCrnAndAccountId(crn, accountId, getEnabledPlatforms()).orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, crn));
    }

    public Credential getByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return repository.findByEnvironmentCrnAndAccountId(environmentCrn, accountId, getEnabledPlatforms())
                .orElseThrow(notFound("Credential with environmentCrn:", environmentCrn));
    }

    public Credential getByEnvironmentNameAndAccountId(String environmentName, String accountId) {
        return repository.findByEnvironmentNameAndAccountId(environmentName, accountId, getEnabledPlatforms())
                .orElseThrow(notFound("Credential with environmentName:", environmentName));
    }

    public Map<String, String> interactiveLogin(String accountId, String userId, Credential credential) {
        validateDeploymentAddress(credential);
        return credentialAdapter.interactiveLogin(credential, accountId, userId);
    }

    public Credential updateByAccountId(Credential credential, String accountId) {
        Credential original = getCredentialAndValidateUpdate(credential, accountId);
        credential.setId(original.getId());
        credential.setAccountId(accountId);
        credential.setResourceCrn(original.getResourceCrn());
        credential.setCreator(original.getCreator());
        Credential updated = repository.save(credentialAdapter.verify(credential, accountId));
        secretService.delete(original.getAttributesSecret());
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_MODIFIED);
        return updated;
    }

    private Credential getCredentialAndValidateUpdate(Credential credential, String accountId) {
        Credential original = repository.findByNameAndAccountId(credential.getName(), accountId, getEnabledPlatforms())
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESSAGE, credential.getName()));
        ValidationResult validationResult = credentialValidator.validateCredentialUpdate(original, credential);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        return original;
    }

    @Retryable(value = BadRequestException.class, maxAttempts = 30, backoff = @Backoff(delay = 2000))
    public void createWithRetry(Credential credential, String accountId, String creator) {
        create(credential, accountId, creator);
    }

    public Credential create(Credential credential, @Nonnull String accountId, @Nonnull String creator) {
        repository.findByNameAndAccountId(credential.getName(), accountId, getEnabledPlatforms())
                .map(Credential::getName)
                .ifPresent(name -> {
                    throw new BadRequestException("Credential already exists with name: " + name);
                });
        credentialValidator.validateCredentialCloudPlatform(credential.getCloudPlatform());
        credentialValidator.validateParameters(Platform.platform(credential.getCloudPlatform()), new Json(credential.getAttributes()));
        credential.setResourceCrn(createCRN(accountId));
        credential.setCreator(creator);
        credential.setAccountId(accountId);
        Credential created = repository.save(credentialAdapter.verify(credential, accountId));
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_CREATED);
        return created;
    }

    public CredentialPrerequisitesResponse getPrerequisites(String cloudPlatform, String deploymentAddress) {
        String cloudPlatformUppercased = cloudPlatform.toUpperCase();
        credentialValidator.validateCredentialCloudPlatform(cloudPlatformUppercased);
        return credentialPrerequisiteService.getPrerequisites(cloudPlatformUppercased, deploymentAddress);
    }

    public String initCodeGrantFlow(String accountId, @Nonnull Credential credential, String userId) {
        repository.findByNameAndAccountId(credential.getName(), accountId, getEnabledPlatforms())
                .map(Credential::getName)
                .ifPresent(name -> {
                    throw new BadRequestException("Credential already exists with name: " + name);
                });
        credentialValidator.validateCredentialCloudPlatform(credential.getCloudPlatform());
        validateDeploymentAddress(credential);
        Credential created = credentialAdapter.initCodeGrantFlow(credential, accountId, userId);
        created.setResourceCrn(createCRN(accountId));
        created.setAccountId(accountId);
        created.setCreator(userId);
        created = repository.save(created);
        return getCodeGrantFlowAppLoginUrl(created);
    }

    public String initCodeGrantFlow(String accountId, String name, String userId) {
        Credential original = repository.findByNameAndAccountId(name, accountId, getEnabledPlatforms())
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESSAGE, name));
        String originalAttributes = original.getAttributes();
        if (getAzureCodeGrantFlowAttributes(original) == null) {
            throw new UnsupportedOperationException("This operation is only allowed on Authorization Code Grant flow based credentails.");
        }
        Credential updated = credentialAdapter.initCodeGrantFlow(original, accountId, userId);
        updated = repository.save(updated);
        secretService.delete(originalAttributes);
        return getCodeGrantFlowAppLoginUrl(updated);
    }

    public Credential authorizeCodeGrantFlow(String code, @Nonnull String state, String accountId, @Nonnull String platform) {
        String cloudPlatformUpperCased = platform.toUpperCase();
        Set<Credential> credentials = repository.findAllByAccountId(accountId, List.of(cloudPlatformUpperCased));
        Credential original = getCredentialByCodeGrantFlowState(state, credentials);
        LOGGER.info("Authorizing credential('{}') with Authorization Code Grant flow.", original.getName());
        String attributesSecret = original.getAttributesSecret();
        updateAuthorizationCodeOfAzureCredential(original, code);
        Credential updated = repository.save(credentialAdapter.verify(original, accountId));
        secretService.delete(attributesSecret);
        return updated;
    }

    private void validateDeploymentAddress(Credential credential) {
        if (getAzureCodeGrantFlowAttributes(credential) == null || StringUtils.isEmpty(getAzureCodeGrantFlowAttributes(credential).getDeploymentAddress())) {
            throw new BadRequestException(DEPLOYMENT_ADDRESS_ATTRIBUTE_NOT_FOUND);
        }
    }

    private String getCodeGrantFlowAppLoginUrl(Credential credential) {
        CodeGrantFlowAttributes azureCodeGrantFlowAttributes = getAzureCodeGrantFlowAttributes(credential);
        Object appLoginUrl = Optional.ofNullable(azureCodeGrantFlowAttributes.getAppLoginUrl())
                .orElseThrow(() -> new CredentialOperationException("Unable to obtain App login url!"));
        return String.valueOf(appLoginUrl);
    }

    private Credential getCredentialByCodeGrantFlowState(@Nonnull String state, Set<Credential> credentials) {
        return credentials.stream()
                .filter(cred -> {
                    CodeGrantFlowAttributes azureCodeGrantFlowAttributes = getAzureCodeGrantFlowAttributes(cred);
                    return azureCodeGrantFlowAttributes != null && state.equalsIgnoreCase(azureCodeGrantFlowAttributes.getCodeGrantFlowState());
                })
                .findFirst()
                .orElseThrow(notFound("Code grant flow based credential for user with state:", state));
    }

    private CodeGrantFlowAttributes getAzureCodeGrantFlowAttributes(Credential credential) {
        CodeGrantFlowAttributes codeGrantFlowAttributes = null;
        try {
            CredentialAttributes credentialAttributes = new Json(credential.getAttributes()).get(CredentialAttributes.class);
            if (credentialAttributes.getAzure() != null && credentialAttributes.getAzure().getCodeGrantFlowBased() != null) {
                codeGrantFlowAttributes = credentialAttributes.getAzure().getCodeGrantFlowBased();
            }
        } catch (IOException e) {
            LOGGER.warn("Attributes of credential '{}' could not get from JSON attributes", credential.getName());
        }
        return codeGrantFlowAttributes;
    }

    private void updateAuthorizationCodeOfAzureCredential(Credential credential, String authorizationCode) {
        try {
            CredentialAttributes credentialAttributes = new Json(credential.getAttributes()).get(CredentialAttributes.class);
            if (credentialAttributes.getAzure() != null && credentialAttributes.getAzure().getCodeGrantFlowBased() != null) {
                CodeGrantFlowAttributes codeGrantFlowAttributes = credentialAttributes.getAzure().getCodeGrantFlowBased();
                codeGrantFlowAttributes.setAuthorizationCode(authorizationCode);
                credential.setAttributes(new Json(credentialAttributes).getValue());
            }
        } catch (IOException e) {
            LOGGER.info("Attributes of credential '{}' could not get from JSON attributes", credential.getName());
        }
    }

    private String createCRN(@Nonnull String accountId) {
        return Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.CREDENTIAL)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }
}
