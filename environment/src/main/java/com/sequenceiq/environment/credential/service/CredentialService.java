package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.common.model.CredentialType.AUDIT;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.credential.verification.CredentialVerification;
import com.sequenceiq.environment.environment.verification.CDPServicePolicyVerification;
import com.sequenceiq.environment.environment.verification.PolicyValidationErrorResponseConverter;

@Service
public class CredentialService implements CompositeAuthResourcePropertyProvider {

    private static final boolean INTERNAL = true;

    private final CredentialValidator credentialValidator;

    private final ServiceProviderCredentialAdapter credentialAdapter;

    private final CredentialPrerequisiteService credentialPrerequisiteService;

    private final PolicyValidationErrorResponseConverter policyValidationErrorResponseConverter;

    private final CredentialCreateService credentialCreateService;

    private final CredentialRetrievalService credentialRetrievalService;

    private final CredentialUpdateService credentialUpdateService;

    public CredentialService(CredentialValidator credentialValidator,
            ServiceProviderCredentialAdapter credentialAdapter,
            CredentialPrerequisiteService credentialPrerequisiteService,
            PolicyValidationErrorResponseConverter policyValidationErrorResponseConverter,
            CredentialCreateService credentialCreateService,
            CredentialRetrievalService credentialRetrievalService,
            CredentialUpdateService credentialUpdateService) {
        this.credentialValidator = credentialValidator;
        this.credentialAdapter = credentialAdapter;
        this.credentialPrerequisiteService = credentialPrerequisiteService;
        this.policyValidationErrorResponseConverter = policyValidationErrorResponseConverter;
        this.credentialCreateService = credentialCreateService;
        this.credentialRetrievalService = credentialRetrievalService;
        this.credentialUpdateService = credentialUpdateService;
    }

    public Credential verify(Credential credential) {
        CredentialVerification verification = credentialAdapter.verify(credential, credential.getAccountId());
        if (verification.isChanged()) {
            return credentialUpdateService.update(verification.getCredential());
        }
        return verification.getCredential();
    }

    public CredentialPrerequisitesResponse getPrerequisites(String cloudPlatform, boolean govCloud,
                                                            String deploymentAddress, CredentialType type) {
        String cloudPlatformInUpperCase = cloudPlatform.toUpperCase(Locale.ROOT);
        credentialValidator.validateCredentialCloudPlatform(cloudPlatformInUpperCase);
        return credentialPrerequisiteService.getPrerequisites(cloudPlatformInUpperCase, govCloud, deploymentAddress, type);
    }

    public CredentialPrerequisitesResponse getInternalPrerequisitesForCloudPlatform(String cloudPlatform, boolean govCloud) {
        credentialValidator.validateCredentialCloudPlatform(cloudPlatform.toUpperCase(Locale.ROOT));
        return credentialPrerequisiteService.getPrerequisites(cloudPlatform, govCloud, null, ENVIRONMENT, INTERNAL);
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

    public PolicyValidationErrorResponses validatePolicy(String accountId, String environmentCrn, List<String> services) {
        Credential credential = getByEnvironmentCrnAndAccountId(environmentCrn, accountId, ENVIRONMENT);
        Map<String, String> experiencePrerequisites = credentialPrerequisiteService.getExperiencePrerequisites(credential.getCloudPlatform());
        CDPServicePolicyVerification cdpServicePolicyVerification = credentialAdapter.verifyByServices(credential, accountId, services, experiencePrerequisites);
        return policyValidationErrorResponseConverter.convert(cdpServicePolicyVerification);
    }

    public Set<Credential> listAvailablesByAccountId(String accountId, CredentialType type) {
        return credentialRetrievalService.listAvailableCredentials(accountId, type);
    }

    public List<ResourceWithId> findAsAuthorizationResourcesInAccountByType(String accountId, CredentialType type) {
        return credentialRetrievalService.findAsAuthorizationResourcesInAccountByType(accountId, type);
    }

    public Set<Credential> findAllById(Iterable<Long> ids) {
        return credentialRetrievalService.findAllById(ids);
    }

    public Credential getByNameForAccountId(String name, String accountId, CredentialType type) {
        return credentialRetrievalService.getByNameForAccountId(name, accountId, type);
    }

    public Optional<Credential> getOptionalByNameForAccountId(String name, String accountId, CredentialType type) {
        return credentialRetrievalService.getOptionalByNameForAccountId(name, accountId, type);
    }

    public Credential getByCrnForAccountId(String crn, String accountId, CredentialType type) {
        return credentialRetrievalService.getByCrnForAccountId(crn, accountId, type);
    }

    public Credential getByCrnForAccountId(String crn, String accountId, CredentialType type, boolean queryArchived) {
        return credentialRetrievalService.getByCrnForAccountId(crn, accountId, type, queryArchived);
    }

    public Optional<Credential> getOptionalByCrnForAccountId(String crn, String accountId, CredentialType type) {
        return credentialRetrievalService.getOptionalByCrnForAccountId(crn, accountId, type);
    }

    public Credential extractCredential(Optional<Credential> credential, String resourceIdentifier) {
        return credentialRetrievalService.extractCredential(credential, resourceIdentifier);
    }

    public Credential getByEnvironmentCrnAndAccountId(String environmentCrn, String accountId, CredentialType type) {
        return credentialRetrievalService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId, type);
    }

    public Credential getByEnvironmentNameAndAccountId(String environmentName, String accountId, CredentialType type) {
        return credentialRetrievalService.getByEnvironmentNameAndAccountId(environmentName, accountId, type);
    }

    public Credential updateByAccountId(Credential credential, String accountId, CredentialType type) {
        return credentialUpdateService.updateByAccountId(credential, accountId, type);
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
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        return credentialRetrievalService.getNamesByCrnsForMessage(crns);
    }

    // Block for creation
    public Credential create(CredentialRequest createCredentialRequest, @Nonnull String accountId, @Nonnull String creatorUserCrn,
            @Nonnull CredentialType type) {
        return credentialCreateService.create(createCredentialRequest, accountId, creatorUserCrn, type);
    }

    public Credential create(Credential credential, @Nonnull String accountId, @Nonnull String creatorUserCrn) {
        return credentialCreateService.create(credential, accountId, creatorUserCrn);
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.CREDENTIAL);
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.CREDENTIAL;
    }
}
