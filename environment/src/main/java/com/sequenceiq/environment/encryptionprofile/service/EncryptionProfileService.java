package com.sequenceiq.environment.encryptionprofile.service;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.respository.EncryptionProfileRepository;

@Service
public class EncryptionProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileService.class);

    private final EncryptionProfileRepository repository;

    private final RegionAwareCrnGenerator regionAwareCrnGenerator;

    private final OwnerAssignmentService ownerAssignmentService;

    private final TransactionService transactionService;

    private final EntitlementService entitlementService;

    public EncryptionProfileService(EncryptionProfileRepository repository,
                                    RegionAwareCrnGenerator regionAwareCrnGenerator,
                                    OwnerAssignmentService ownerAssignmentService,
                                    TransactionService transactionService,
                                    EntitlementService entitlementService) {
        this.repository = repository;
        this.regionAwareCrnGenerator = regionAwareCrnGenerator;
        this.ownerAssignmentService = ownerAssignmentService;
        this.transactionService = transactionService;
        this.entitlementService = entitlementService;
    }

    public EncryptionProfile create(EncryptionProfile encryptionProfile, String accountId, String creator) {
        if (!entitlementService.isConfigureEncryptionProfileEnabled(accountId)) {
            throw new BadRequestException("Encryption profile creation is not enabled for account: " + accountId);
        }
        LOGGER.debug("Create encryption profile request has received: {}", encryptionProfile);
        repository.findByNameAndAccountId(encryptionProfile.getName(), accountId)
                .map(EncryptionProfile::getName)
                .ifPresent(name -> {
                    throw new BadRequestException("Encryption Profile already exists with name: " + name);
                });

        encryptionProfile.setResourceCrn(createCRN(accountId));
        encryptionProfile.setAccountId(accountId);
        ownerAssignmentService.assignResourceOwnerRoleIfEntitled(creator, encryptionProfile.getResourceCrn());
        try {
            return transactionService.required(() -> repository.save(encryptionProfile));
        } catch (TransactionService.TransactionExecutionException e) {
            ownerAssignmentService.notifyResourceDeleted(encryptionProfile.getResourceCrn());
            LOGGER.error("Error happened during encryption profile creation: ", e);
            throw new InternalServerErrorException(e);
        }
    }

    private String createCRN(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.ENCYRPTION_PROFILE, accountId);
    }
}
