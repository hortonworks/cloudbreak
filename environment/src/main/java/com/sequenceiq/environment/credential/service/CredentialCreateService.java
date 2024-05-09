package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.common.model.CredentialType.AUDIT;

import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.domain.CredentialSettings;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.v1.converter.CreateCredentialRequestToCredentialConverter;
import com.sequenceiq.environment.credential.validation.CredentialValidator;

@Service
public class CredentialCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialCreateService.class);

    @Inject
    private CredentialRepository repository;

    @Inject
    private CredentialValidator credentialValidator;

    @Inject
    private CreateCredentialRequestToCredentialConverter credentialRequestConverter;

    @Inject
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private CredentialNotificationService credentialNotificationService;

    @Value("${environment.enabledplatforms}")
    private Set<String> enabledPlatforms;

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
        repository.findByNameAndAccountId(credential.getName(), accountId, enabledPlatforms, credential.getType())
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
            credentialNotificationService.send(createdCredential, ResourceEvent.CREDENTIAL_CREATED);
            return createdCredential;
        } catch (TransactionService.TransactionExecutionException e) {
            ownerAssignmentService.notifyResourceDeleted(credentialCrn);
            LOGGER.error("Error happened during credential creation: ", e);
            throw new InternalServerErrorException(e);
        }
    }

    private String createCRN(@Nonnull String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.CREDENTIAL, accountId);
    }

}
