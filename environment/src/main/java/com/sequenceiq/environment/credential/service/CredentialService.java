package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.common.model.CredentialType.AUDIT;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
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
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.domain.CredentialSettings;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.v1.converter.CreateCredentialRequestToCredentialConverter;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.credential.verification.CredentialVerification;
import com.sequenceiq.environment.environment.verification.CDPServicePolicyVerification;
import com.sequenceiq.environment.environment.verification.PolicyValidationErrorResponseConverter;
import com.sequenceiq.notification.NotificationSender;

@Service
public class CredentialService extends AbstractCredentialService implements CompositeAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

    @Inject
    private CredentialRepository repository;

    @Inject
    private CredentialValidator credentialValidator;

    @Inject
    private CreateCredentialRequestToCredentialConverter credentialRequestConverter;

    @Inject
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Inject
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @Inject
    private SecretService secretService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private PolicyValidationErrorResponseConverter policyValidationErrorResponseConverter;

    protected CredentialService(NotificationSender notificationSender, CloudbreakMessagesService messagesService,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms) {
        super(notificationSender, messagesService, enabledPlatforms);
    }

    public Set<Credential> listAvailablesByAccountId(String accountId, CredentialType type) {
        return repository.findAllByAccountId(accountId, getValidPlatformsForAccountId(accountId, type), type);
    }

    public List<ResourceWithId> findAsAuthorizationResourcesInAccountByType(String accountId, CredentialType type) {
        return repository.findAsAuthorizationResourcesInAccountByType(accountId, getValidPlatformsForAccountId(accountId, type), type);
    }

    public Set<Credential> findAllById(Iterable<Long> ids) {
        return Sets.newLinkedHashSet(repository.findAllById(ids));
    }

    @Cacheable(cacheNames = "credentialCloudPlatformCache")
    public Set<String> getValidPlatformsForAccountId(String accountId, CredentialType type) {
        return getEnabledPlatforms()
                .stream()
                .filter(cloudPlatform -> credentialValidator.isCredentialCloudPlatformValid(cloudPlatform, accountId, type))
                .collect(Collectors.toSet());
    }

    public Credential getByNameForAccountId(String name, String accountId, CredentialType type) {
        return extractCredential(getOptionalByNameForAccountId(name, accountId, type), name);
    }

    public Optional<Credential> getOptionalByNameForAccountId(String name, String accountId, CredentialType type) {
        return repository.findByNameAndAccountId(name, accountId, getEnabledPlatforms(), type);
    }

    public Credential getByCrnForAccountId(String crn, String accountId, CredentialType type) {
        return getByCrnForAccountId(crn, accountId, type, false);
    }

    public Credential getByCrnForAccountId(String crn, String accountId, CredentialType type, boolean queryArchived) {
        return extractCredential(getOptionalByCrnForAccountId(crn, accountId, type, queryArchived), crn);
    }

    public Optional<Credential> getOptionalByCrnForAccountId(String crn, String accountId, CredentialType type) {
        return getOptionalByCrnForAccountId(crn, accountId, type, false);
    }

    public Optional<Credential> getOptionalByCrnForAccountId(String crn, String accountId, CredentialType type, boolean queryArchived) {
        Optional<Credential> credential = repository.findByCrnAndAccountId(crn, accountId, getEnabledPlatforms(), type, false);
        if (queryArchived && credential.isEmpty()) {
            credential = repository.findByCrnAndAccountId(crn, accountId, getEnabledPlatforms(), type, true);
        }
        return credential;
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

        Credential updated = repository.save(credentialAdapter.verify(credential, accountId, true).getCredential());
        secretService.deleteByVaultSecretJson(original.getAttributesSecret());
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

    public Credential create(CredentialRequest createCredentialRequest, @Nonnull String accountId, @Nonnull String creatorUserCrn,
            @Nonnull CredentialType type) {
        LOGGER.debug("Create credential request has received: {}", createCredentialRequest);

        LOGGER.debug("Validating credential for cloudPlatform {} and creator {}.", createCredentialRequest.getCloudPlatform(), creatorUserCrn);
        credentialValidator.validateCredentialCloudPlatform(createCredentialRequest.getCloudPlatform(), creatorUserCrn, type);

        LOGGER.debug("Validating credential for cloudPlatform {} and creator {}.", createCredentialRequest.getCloudPlatform(), creatorUserCrn);
        credentialValidator.validateCreate(createCredentialRequest);

        Credential credential = credentialRequestConverter.convert(createCredentialRequest);
        credential.setType(type);
        if (type == AUDIT) {
            // Permission verification is disabled due to CB-9955
            credential.setCredentialSettings(new CredentialSettings(false, false));
        }
        return create(credential, accountId, creatorUserCrn);
    }

    public Credential create(Credential credential, @Nonnull String accountId, @Nonnull String creatorUserCrn) {
        repository.findByNameAndAccountId(credential.getName(), accountId, getEnabledPlatforms(), credential.getType())
                .map(Credential::getName)
                .ifPresent(name -> {
                    throw new BadRequestException("Credential already exists with name: " + name);
                });
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
        ownerAssignmentService.assignResourceOwnerRoleIfEntitled(creatorUserCrn, credentialCrn);
        try {
            Credential createdCredential = transactionService.required(() -> repository.save(verifiedCredential));
            sendCredentialNotification(createdCredential, ResourceEvent.CREDENTIAL_CREATED);
            return createdCredential;
        } catch (TransactionService.TransactionExecutionException e) {
            ownerAssignmentService.notifyResourceDeleted(credentialCrn);
            LOGGER.error("Error happened during credential creation: ", e);
            throw new InternalServerErrorException(e);
        }
    }

    public CredentialPrerequisitesResponse getPrerequisites(String cloudPlatform, boolean govCloud,
            String deploymentAddress, String userCrn, CredentialType type) {
        String cloudPlatformInUpperCase = cloudPlatform.toUpperCase(Locale.ROOT);
        credentialValidator.validateCredentialCloudPlatform(cloudPlatformInUpperCase, userCrn, type);
        return credentialPrerequisiteService.getPrerequisites(cloudPlatformInUpperCase, govCloud, deploymentAddress, type);
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

    Optional<Credential> findByNameAndAccountId(String name, String accountId, Collection<String> cloudPlatforms, CredentialType type) {
        return repository.findByNameAndAccountId(name, accountId, cloudPlatforms, type);
    }

    Optional<Credential> findByCrnAndAccountId(String crn, String accountId, Collection<String> cloudPlatforms,
            CredentialType type) {
        return repository.findByCrnAndAccountId(crn, accountId, cloudPlatforms, type, false);
    }

    Credential save(Credential credential) {
        return repository.save(credential);
    }

    private String createCRN(@Nonnull String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.CREDENTIAL, accountId);
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
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.CREDENTIAL;
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        Map<String, Optional<String>> result = new HashMap<>();
        repository.findResourceNamesByCrnAndAccountId(crns, ThreadBasedUserCrnProvider.getAccountId(), getEnabledPlatforms()).stream()
                .forEach(nameAndCrn -> result.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        return result;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.CREDENTIAL);
    }

    public PolicyValidationErrorResponses validatePolicy(String accountId, String environmentCrn, List<String> services) {
        Credential credential = getByEnvironmentCrnAndAccountId(environmentCrn, accountId, ENVIRONMENT);
        Map<String, String> experiencePrerequisites = credentialPrerequisiteService.getExperiencePrerequisites(credential.getCloudPlatform());
        CDPServicePolicyVerification cdpServicePolicyVerification = credentialAdapter.verifyByServices(credential, accountId, services, experiencePrerequisites);
        return policyValidationErrorResponseConverter.convert(cdpServicePolicyVerification);
    }
}
