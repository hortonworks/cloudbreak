package com.sequenceiq.environment.encryptionprofile.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.TlsVersionResponse;
import com.sequenceiq.environment.encryptionprofile.cache.DefaultEncryptionProfileProvider;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.respository.EncryptionProfileRepository;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.cluster.ClusterService;

@Service
public class EncryptionProfileService implements CompositeAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileService.class);

    private static final String RESERVED_NAME = "cdp_default";

    private static final String RSA = "RSA";

    @Value("${cb.encryptionprofile.default.name:cdp_default_fips_v1}")
    private String defaultEncryptionProfileName;

    private final EncryptionProfileRepository repository;

    private final RegionAwareCrnGenerator regionAwareCrnGenerator;

    private final OwnerAssignmentService ownerAssignmentService;

    private final TransactionService transactionService;

    private final EncryptionProfileProvider encryptionProfileProvider;

    private final DefaultEncryptionProfileProvider defaultEncryptionProfileProvider;

    private final ClusterService clusterService;

    public EncryptionProfileService(
            EncryptionProfileRepository repository,
            RegionAwareCrnGenerator regionAwareCrnGenerator,
            OwnerAssignmentService ownerAssignmentService,
            TransactionService transactionService,
            EncryptionProfileProvider encryptionProfileProvider,
            DefaultEncryptionProfileProvider defaultEncryptionProfileProvider,
            ClusterService clusterService
    ) {
        this.repository = repository;
        this.regionAwareCrnGenerator = regionAwareCrnGenerator;
        this.ownerAssignmentService = ownerAssignmentService;
        this.transactionService = transactionService;
        this.encryptionProfileProvider = encryptionProfileProvider;
        this.defaultEncryptionProfileProvider = defaultEncryptionProfileProvider;
        this.clusterService = clusterService;
    }

    public EncryptionProfile create(EncryptionProfile encryptionProfile, String accountId, String creator) {
        LOGGER.debug("Create encryption profile request has received: {}", encryptionProfile);
        if (encryptionProfile.getName().toLowerCase(Locale.ROOT).startsWith(RESERVED_NAME)) {
            throw new BadRequestException("Encryption Profile name cannot start with: " + RESERVED_NAME);
        }
        repository.findByNameAndAccountId(encryptionProfile.getName(), accountId)
                .map(EncryptionProfile::getName)
                .ifPresent(name -> {
                    throw new BadRequestException("Encryption Profile already exists with name: " + name);
                });

        if (CollectionUtils.isEmpty(encryptionProfile.getCipherSuites())) {
            throw new BadRequestException("Cipher suites list cannot be empty");
        }

        if (!isTls13Only(encryptionProfile.getTlsVersions())
                && !containsRsaCipherSuite(encryptionProfile.getCipherSuites(), encryptionProfile.getTlsVersions())) {
            throw new BadRequestException("Encryption Profile must have at least one RSA cipher suite");
        }

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

    private boolean isTls13Only(Set<TlsVersion> tlsVersions) {
        return  tlsVersions.contains(TlsVersion.TLS_1_3) && tlsVersions.size() == 1;
    }

    private boolean containsRsaCipherSuite(List<String> cipherSuites, Set<TlsVersion> tlsVersions) {
        return cipherSuites
                .stream()
                .anyMatch(EncryptionProfileService::isRsa);
    }

    public static boolean isRsa(String cipherSuite) {
        if (cipherSuite == null) {
            return false;
        }
        return cipherSuite.contains(RSA);
    }

    public EncryptionProfile getByNameAndAccountId(String encryptionProfileName, String accountId) {
        if (StringUtils.isNotEmpty(encryptionProfileName)) {
            return getEncryptionProfileByName(encryptionProfileName, accountId);
        } else {
            return getClouderaDefaultEncryptionProfile();
        }
    }

    private EncryptionProfile getEncryptionProfileByName(String encryptionProfileName, String accountId) {
        Map<String, EncryptionProfile> defaultEncryptionProfileMap = defaultEncryptionProfileProvider.defaultEncryptionProfilesByName();
        Optional<EncryptionProfile> encryptionProfileOp = repository.findByNameAndAccountId(encryptionProfileName, accountId);
        if (encryptionProfileOp.isPresent()) {
            return encryptionProfileOp.get();
        } else {
            EncryptionProfile encryptionProfile = defaultEncryptionProfileMap.get(encryptionProfileName);
            if (encryptionProfile == null) {
                throw new NotFoundException("Encryption Profile not found with name: " + encryptionProfileName);
            }
            return encryptionProfile;
        }
    }

    public EncryptionProfile getClouderaDefaultEncryptionProfile() {
        Map<String, EncryptionProfile> defaultEncryptionProfileMap = defaultEncryptionProfileProvider.defaultEncryptionProfilesByName();
        return defaultEncryptionProfileMap.get(defaultEncryptionProfileName);
    }

    public List<EncryptionProfile> getAllDefaultEncryptionProfiles() {
        return defaultEncryptionProfileProvider
                .defaultEncryptionProfilesByName()
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    public EncryptionProfile getByCrnOrDefault(String encryptionProfileCrn) {
        if (StringUtils.isNotEmpty(encryptionProfileCrn)) {
            return getByCrn(encryptionProfileCrn);
        } else {
            return getClouderaDefaultEncryptionProfile();
        }
    }

    public EncryptionProfile getByCrn(String encryptionProfileCrn) {
        Optional<EncryptionProfile> encryptionProfileOp = repository.findByResourceCrn(encryptionProfileCrn);
        if (encryptionProfileOp.isPresent()) {
            return encryptionProfileOp.get();
        } else {
            Map<String, EncryptionProfile> defaultEncryptionProfileMap = defaultEncryptionProfileProvider.defaultEncryptionProfilesByCrn();
            EncryptionProfile encryptionProfile = defaultEncryptionProfileMap.get(encryptionProfileCrn);
            if (encryptionProfile == null) {
                throw new NotFoundException("Encryption Profile not found with crn: " + encryptionProfileCrn);
            }
            return encryptionProfile;
        }
    }

    public EncryptionProfile deleteByNameAndAccountId(String encryptionProfileName, String accountId) {
        LOGGER.debug("Delete encryption profile with name: {} is received.", encryptionProfileName);

        EncryptionProfile encryptionProfile = repository
                .findByNameAndAccountId(encryptionProfileName, accountId)
                .orElseThrow(notFound("Encryption profile", encryptionProfileName));

        checkEncryptionProfileCanBeDeleted(encryptionProfile);

        repository.delete(encryptionProfile);
        ownerAssignmentService.notifyResourceDeleted(encryptionProfile.getResourceCrn());

        return encryptionProfile;
    }

    public EncryptionProfile deleteByResourceCrn(String crn) {
        LOGGER.debug("Delete encryption profile with CRN: {} is received.", crn);

        EncryptionProfile encryptionProfile = repository.findByResourceCrn(crn)
                .orElseThrow(notFound("Encryption profile with crn", crn));

        checkEncryptionProfileCanBeDeleted(encryptionProfile);

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

    private void checkEncryptionProfileCanBeDeleted(EncryptionProfile encryptionProfile) {
        if (ResourceStatus.DEFAULT.equals(encryptionProfile.getResourceStatus())) {
            throw new BadRequestException("The default encryption profile cannot be deleted.");
        }

        List<String> envNames = repository.findAllEnvNamesByEncryptionProfileCrn(encryptionProfile.getResourceCrn());
        List<String> clusterNames = clusterService.getClustersNamesByEncryptionProfile(encryptionProfile.getResourceCrn());

        StringBuilder message = new StringBuilder();

        if (!envNames.isEmpty()) {
            message.append(" It is still used by the environment(s): ")
                    .append(String.join(",", envNames))
                    .append('.');
        }
        if (!clusterNames.isEmpty()) {
            message.append(" It is still used by the cluster(s): ")
                    .append(String.join(",", clusterNames))
                    .append('.');

        }

        if (!message.isEmpty()) {
            throw new BadRequestException(String.format("Encryption Profile cannot be deleted.%s", message));
        }
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        List<String> crns = new ArrayList<>(repository.findAllResourceCrnByNameListAndAccountId(resourceNames, ThreadBasedUserCrnProvider.getAccountId()));
        Map<String, EncryptionProfile> defaultsByName = defaultEncryptionProfileProvider.defaultEncryptionProfilesByName();
        resourceNames.stream()
                .filter(defaultsByName::containsKey)
                .map(name -> defaultsByName.get(name).getResourceCrn())
                .forEach(crns::add);
        return crns;
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        Optional<String> resourceCrn = repository.findResourceCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId());
        if (resourceCrn.isPresent()) {
            return resourceCrn.get();
        }
        EncryptionProfile defaultProfile = defaultEncryptionProfileProvider.defaultEncryptionProfilesByName().get(resourceName);
        if (defaultProfile != null) {
            return defaultProfile.getResourceCrn();
        }
        throw notFound("Encryption profile", resourceName).get();
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.ENCRYPTION_PROFILE;
    }

    public Set<TlsVersionResponse> listCiphersByTlsVersion() {
        Map<String, List<String>> ciphersByTls = encryptionProfileProvider.getAllCipherSuitesAvailableByTlsVersion();
        Map<String, List<String>> recommendedCipherSuites = encryptionProfileProvider.getRecommendedCipherSuites();

        return ciphersByTls.entrySet()
                .stream()
                .map(entry -> new TlsVersionResponse(entry.getKey(), new ArrayList<>(entry.getValue()), recommendedCipherSuites.get(entry.getKey())))
                .collect(Collectors.toSet());
    }

    public void setEncryptionProfile(Environment environment, String encryptionProfileCrn) {
        LOGGER.info("Enabling encryption profile for environment {}, env crn: {}, encryption profile crn: {}", environment.getName(),
                environment.getResourceCrn(), encryptionProfileCrn);
        EncryptionProfile encryptionProfile = getByCrn(encryptionProfileCrn);
        environment.setEncryptionProfileCrn(encryptionProfile.getResourceCrn());
    }

    public EncryptionProfile getEncryptionProfileByNameOrCrn(String encryptionProfileNameOrCrn) {
        EncryptionProfile encryptionProfile = null;
        if (StringUtils.isNotBlank(encryptionProfileNameOrCrn)) {
            if (Crn.isCrn(encryptionProfileNameOrCrn)) {
                encryptionProfile = getByCrnOrDefault(encryptionProfileNameOrCrn);
            } else {
                encryptionProfile = getByNameAndAccountId(encryptionProfileNameOrCrn, ThreadBasedUserCrnProvider.getAccountId());
            }
        }

        return encryptionProfile;

    }
}
