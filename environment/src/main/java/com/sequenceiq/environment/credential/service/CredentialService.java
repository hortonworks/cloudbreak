package com.sequenceiq.environment.credential.service;


import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.common.model.CredentialType.AUDIT;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.cloudera.cdp.environments.model.CreateAWSCredentialRequest;
import com.google.common.base.Strings;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.ResourceBasedCrnProvider;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.CodeGrantFlowAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.exception.CredentialOperationException;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.v1.converter.CredentialRequestToCreateAWSCredentialRequestConverter;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.credential.verification.CredentialVerification;
import com.sequenceiq.notification.NotificationSender;

@Service
public class CredentialService extends AbstractCredentialService implements ResourceBasedCrnProvider {

    private static final String DEPLOYMENT_ADDRESS_ATTRIBUTE_NOT_FOUND = "The 'deploymentAddress' parameter needs to be specified in the interactive login "
            + "request!";

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

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

    @Inject
    private CredentialRequestToCreateAWSCredentialRequestConverter credentialRequestToCreateAWSCredentialRequestConverter;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private TransactionService transactionService;

    protected CredentialService(NotificationSender notificationSender, CloudbreakMessagesService messagesService,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms) {
        super(notificationSender, messagesService, enabledPlatforms);
    }

    public Set<Credential> listAvailablesByAccountId(String accountId, CredentialType type) {
        return repository.findAllByAccountId(accountId, getValidPlatformsForAccountId(accountId), type);
    }

    @Cacheable(cacheNames = "credentialCloudPlatformCache")
    public Set<String> getValidPlatformsForAccountId(String accountId) {
        return getEnabledPlatforms()
                .stream()
                .filter(cloudPlatform -> credentialValidator.isCredentialCloudPlatformValid(cloudPlatform, accountId))
                .collect(Collectors.toSet());
    }

    public Credential getByNameForAccountId(String name, String accountId, CredentialType type) {
        return extractCredential(getOptionalByNameForAccountId(name, accountId, type), name);
    }

    public Optional<Credential> getOptionalByNameForAccountId(String name, String accountId, CredentialType type) {
        return repository.findByNameAndAccountId(name, accountId, getEnabledPlatforms(), type);
    }

    public Credential getByCrnForAccountId(String crn, String accountId, CredentialType type) {
        return extractCredential(getOptionalByCrnForAccountId(crn, accountId, type), crn);
    }

    public Optional<Credential> getOptionalByCrnForAccountId(String crn, String accountId, CredentialType type) {
        return repository.findByCrnAndAccountId(crn, accountId, getEnabledPlatforms(), type);
    }

    public Credential extractCredential(Optional<Credential> credential, String resourceIdentifier) {
        return credential.orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, resourceIdentifier));
    }

    public Credential getByEnvironmentCrnAndAccountId(String environmentCrn, String accountId, CredentialType type) {
        return repository.findByEnvironmentCrnAndAccountId(environmentCrn, accountId, getEnabledPlatforms(), type)
                .orElseThrow(notFound("Credential with environmentCrn:", environmentCrn));
    }

    public Credential getByEnvironmentNameAndAccountId(String environmentName, String accountId, CredentialType type) {
        return repository.findByEnvironmentNameAndAccountId(environmentName, accountId, getEnabledPlatforms(), type)
                .orElseThrow(notFound("Credential with environmentName:", environmentName));
    }

    public Map<String, String> interactiveLogin(String accountId, String userId, Credential credential) {
        validateDeploymentAddress(credential);
        return credentialAdapter.interactiveLogin(credential, accountId, userId);
    }

    public Credential verify(Credential credential) {
        CredentialVerification verification = credentialAdapter.verify(credential, credential.getAccountId());
        if (verification.isChanged()) {
            return repository.save(verification.getCredential());
        }
        return verification.getCredential();
    }

    public Credential updateByAccountId(Credential credential, String accountId, CredentialType type) {
        Credential original = getCredentialAndValidateUpdate(credential, accountId, type);
        credential.setId(original.getId());
        credential.setAccountId(accountId);
        credential.setResourceCrn(original.getResourceCrn());
        credential.setCreator(original.getCreator());
        Credential updated = repository.save(credentialAdapter.verify(credential, accountId).getCredential());
        secretService.delete(original.getAttributesSecret());
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_MODIFIED);
        return updated;
    }

    private Credential getCredentialAndValidateUpdate(Credential credential, String accountId, CredentialType type) {
        Credential original = repository.findByNameAndAccountId(credential.getName(), accountId, getEnabledPlatforms(), type)
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, credential.getName()));
        ValidationResult validationResult = credentialValidator.validateCredentialUpdate(original, credential,
                type);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        return original;
    }

    @Retryable(value = BadRequestException.class, maxAttempts = 30, backoff = @Backoff(delay = 2000))
    public void createWithRetry(Credential credential, String accountId, String creatorUserCrn) {
        create(credential, accountId, creatorUserCrn, ENVIRONMENT);
    }

    public Credential create(Credential credential, @Nonnull String accountId, @Nonnull String creatorUserCrn, CredentialType type) {
        repository.findByNameAndAccountId(credential.getName(), accountId, getEnabledPlatforms(), type)
                .map(Credential::getName)
                .ifPresent(name -> {
                    throw new BadRequestException("Credential already exists with name: " + name);
                });
        LOGGER.debug("Validating credential for cloudPlatform {} and creator {}.", credential.getCloudPlatform(), creatorUserCrn);
        credentialValidator.validateCredentialCloudPlatform(credential.getCloudPlatform(), creatorUserCrn);
        LOGGER.debug("Validating credential parameters for cloudPlatform {} and creator {}.", credential.getCloudPlatform(), creatorUserCrn);
        credentialValidator.validateParameters(Platform.platform(credential.getCloudPlatform()), new Json(credential.getAttributes()));
        String credentialCrn = createCRN(accountId);
        credential.setResourceCrn(credentialCrn);
        credential.setCreator(creatorUserCrn);
        credential.setAccountId(accountId);
        Credential verifiedCredential = credentialAdapter.verify(credential, accountId, Boolean.TRUE).getCredential();
        if (verifiedCredential.getVerificationStatusText() != null) {
            throw new BadRequestException(verifiedCredential.getVerificationStatusText());
        }
        try {
            Credential createdCredential = transactionService.required(() -> {
                Credential created = repository.save(verifiedCredential);
                ownerAssignmentService.assignResourceOwnerRoleIfEntitled(creatorUserCrn, credentialCrn, accountId);
                return created;
            });
            sendCredentialNotification(createdCredential, ResourceEvent.CREDENTIAL_CREATED);
            return createdCredential;
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Error happened during credential creation: ", e);
            throw new InternalServerErrorException(e);
        }
    }

    public CredentialPrerequisitesResponse getPrerequisites(String cloudPlatform, String deploymentAddress, String userCrn, CredentialType type) {
        String cloudPlatformUppercased = cloudPlatform.toUpperCase();
        credentialValidator.validateCredentialCloudPlatform(cloudPlatformUppercased, userCrn);
        return credentialPrerequisiteService.getPrerequisites(cloudPlatformUppercased, deploymentAddress, type);
    }

    public String initCodeGrantFlow(String accountId, @Nonnull Credential credential, String creatorUserCrn) {
        repository.findByNameAndAccountId(credential.getName(), accountId, getEnabledPlatforms(), ENVIRONMENT)
                .map(Credential::getName)
                .ifPresent(name -> {
                    throw new BadRequestException("Credential already exists with name: " + name);
                });
        LOGGER.debug("Validating credential for cloudPlatform {} and creator {}.", credential.getCloudPlatform(), creatorUserCrn);
        credentialValidator.validateCredentialCloudPlatform(credential.getCloudPlatform(), creatorUserCrn);
        validateDeploymentAddress(credential);
        Credential created = credentialAdapter.initCodeGrantFlow(credential, accountId, creatorUserCrn);
        created.setResourceCrn(createCRN(accountId));
        created.setAccountId(accountId);
        created.setCreator(creatorUserCrn);
        created = repository.save(created);
        return getCodeGrantFlowAppLoginUrl(created);
    }

    public String initCodeGrantFlow(String accountId, String name, String userId) {
        Credential original = repository.findByNameAndAccountId(name, accountId, getEnabledPlatforms(), ENVIRONMENT)
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, name));
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
        Set<Credential> credentials = repository.findAllByAccountId(accountId, List.of(cloudPlatformUpperCased), ENVIRONMENT);
        Credential original = getCredentialByCodeGrantFlowState(state, credentials);
        LOGGER.info("Authorizing credential('{}') with Authorization Code Grant flow.", original.getName());
        String attributesSecret = original.getAttributesSecret();
        updateAuthorizationCodeOfAzureCredential(original, code);
        Credential updated = repository.save(credentialAdapter.verify(original, accountId).getCredential());
        secretService.delete(attributesSecret);
        return updated;
    }

    public String getCloudPlatformByCredential(String credentialName, String accountId, CredentialType type) {
        if (!Strings.isNullOrEmpty(credentialName)) {
            try {
                Credential credential = getByNameForAccountId(credentialName, accountId, type);
                return credential.getCloudPlatform();
            } catch (NotFoundException e) {
                throw new BadRequestException(String.format("No credential found with name [%s] in the workspace.",
                        credentialName), e);
            }
        } else {
            throw new BadRequestException("No credential has been specified as part of environment creation.");
        }
    }

    public CreateAWSCredentialRequest getCreateAWSCredentialForCli(CredentialRequest credentialRequest) {
        ValidationResult validationResult = credentialValidator.validateAwsCredentialRequest(credentialRequest);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        return credentialRequestToCreateAWSCredentialRequestConverter.convert(credentialRequest);
    }

    Optional<Credential> findByNameAndAccountId(String name, String accountId, Collection<String> cloudPlatforms, CredentialType type) {
        return repository.findByNameAndAccountId(name, accountId, cloudPlatforms, type);
    }

    Optional<Credential> findByCrnAndAccountId(String crn, String accountId, Collection<String> cloudPlatforms, CredentialType type) {
        return repository.findByCrnAndAccountId(crn, accountId, cloudPlatforms, type);
    }

    Credential save(Credential credential) {
        return repository.save(credential);
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

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        Optional<Credential> credential = getOptionalByNameForAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId(), ENVIRONMENT);
        if (credential.isEmpty()) {
            credential = getOptionalByNameForAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId(), AUDIT);
        }
        return extractCredential(credential, resourceName).getResourceCrn();
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return resourceNames.stream()
                .map(resourceName -> getByNameForAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId(),
                        ENVIRONMENT).getResourceCrn())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getResourceCrnsInAccount() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return repository.findAllResourceCrnsByAccountId(accountId, ENVIRONMENT);
    }

    @Override
    public AuthorizationResourceType getResourceType() {
        return AuthorizationResourceType.CREDENTIAL;
    }
}
