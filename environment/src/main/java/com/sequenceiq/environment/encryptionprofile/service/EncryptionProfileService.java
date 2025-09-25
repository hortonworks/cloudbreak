package com.sequenceiq.environment.encryptionprofile.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.tls.DefaultEncryptionProfileProvider;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.TlsVersionResponse;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.respository.EncryptionProfileRepository;

@Service
public class EncryptionProfileService implements CompositeAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileService.class);

    private final EncryptionProfileRepository repository;

    private final RegionAwareCrnGenerator regionAwareCrnGenerator;

    private final OwnerAssignmentService ownerAssignmentService;

    private final TransactionService transactionService;

    private final DefaultEncryptionProfileProvider defaultEncryptionProfileProvider;

    public EncryptionProfileService(
            EncryptionProfileRepository repository,
            RegionAwareCrnGenerator regionAwareCrnGenerator,
            OwnerAssignmentService ownerAssignmentService,
            TransactionService transactionService,
            DefaultEncryptionProfileProvider defaultEncryptionProfileProvider
    ) {
        this.repository = repository;
        this.regionAwareCrnGenerator = regionAwareCrnGenerator;
        this.ownerAssignmentService = ownerAssignmentService;
        this.transactionService = transactionService;
        this.defaultEncryptionProfileProvider = defaultEncryptionProfileProvider;
    }

    public EncryptionProfile create(EncryptionProfile encryptionProfile, String accountId, String creator) {
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

    public EncryptionProfile getByNameAndAccountId(String encryptionProfileName, String accountId) {
        return repository.findByNameAndAccountId(encryptionProfileName, accountId)
                .orElseThrow(notFound("Encryption profile", encryptionProfileName));
    }

    public EncryptionProfile getByCrn(String encryptionProfileCrn) {
        return repository.findByResourceCrn(encryptionProfileCrn)
                .orElseThrow(notFound("Encryption profile with crn", encryptionProfileCrn));
    }

    public EncryptionProfile deleteByNameAndAccountId(String encryptionProfileName, String accountId) {
        LOGGER.debug("Delete encryption profile with name: {} is received.", encryptionProfileName);

        EncryptionProfile encryptionProfile = repository
                .findByNameAndAccountId(encryptionProfileName, accountId)
                .orElseThrow(notFound("Encryption profile", encryptionProfileName));

        checkEncryptionProfileCanBeDeleted(encryptionProfile, encryptionProfile.getAccountId());

        repository.delete(encryptionProfile);
        ownerAssignmentService.notifyResourceDeleted(encryptionProfile.getResourceCrn());

        return encryptionProfile;
    }

    public EncryptionProfile deleteByResourceCrn(String crn) {
        LOGGER.debug("Delete encryption profile with CRN: {} is received.", crn);

        EncryptionProfile encryptionProfile = repository.findByResourceCrn(crn)
                .orElseThrow(notFound("Encryption profile with crn", crn));

        checkEncryptionProfileCanBeDeleted(encryptionProfile, encryptionProfile.getAccountId());

        repository.delete(encryptionProfile);
        ownerAssignmentService.notifyResourceDeleted(encryptionProfile.getResourceCrn());
        return encryptionProfile;
    }

    public List<ResourceWithId> getEncryptionProfilesAsAuthorizationResources() {
        return repository.findAuthorizationResourcesByAccountId(ThreadBasedUserCrnProvider.getAccountId());
    }

    public List<EncryptionProfile> findAllById(List<Long> authorizedResourceIds) {
        return repository.findAllById(authorizedResourceIds);
    }

    public List<EncryptionProfile> listAll() {
        return repository.findAllByAccountId(ThreadBasedUserCrnProvider.getAccountId());
    }

    private String createCRN(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.ENCYRPTION_PROFILE, accountId);
    }

    private void checkEncryptionProfileCanBeDeleted(EncryptionProfile encryptionProfile, String accountId) {
        if (ResourceStatus.DEFAULT.equals(encryptionProfile.getResourceStatus())) {
            throw new BadRequestException("The default encryption profile cannot be deleted.");
        }

        List<String> names = repository.findAllEnvNamesByEncrytionProfileNameAndAccountId(encryptionProfile.getName(), accountId);

        if (!names.isEmpty()) {
            throw new BadRequestException("Encryption Profile cannot be deleted. It is still used by the environment(s): " + String.join(",", names));
        }
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return repository.findAllResourceCrnByNameListAndAccountId(resourceNames, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return repository.findResourceCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(notFound("Encryption profile", resourceName));
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.ENCRYPTION_PROFILE;
    }

    public Set<TlsVersionResponse> listCiphersByTlsVersion() {
        Map<String, List<String>> ciphersByTls = defaultEncryptionProfileProvider.getAllCipherSuitesAvailableByTlsVersion();
        Map<String, List<String>> recommendedCipherSuites = defaultEncryptionProfileProvider.getRecommendedCipherSuites();

        return ciphersByTls.entrySet()
                .stream()
                .map(entry -> new TlsVersionResponse(entry.getKey(), new ArrayList<>(entry.getValue()), recommendedCipherSuites.get(entry.getKey())))
                .collect(Collectors.toSet());
    }
}
