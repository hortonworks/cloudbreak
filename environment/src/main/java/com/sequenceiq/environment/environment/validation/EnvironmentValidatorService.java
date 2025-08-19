package com.sequenceiq.environment.environment.validation;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.util.SecurityGroupSeparator.getSecurityGroupIds;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.recipe.EnvironmentRecipeService;
import com.sequenceiq.environment.environment.service.validation.SeLinuxValidationService;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyArnValidator;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyUrlValidator;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyValidator;
import com.sequenceiq.environment.environment.validation.validators.ManagedIdentityRoleValidator;
import com.sequenceiq.environment.environment.validation.validators.NetworkValidator;
import com.sequenceiq.environment.environment.validation.validators.PublicKeyValidator;
import com.sequenceiq.environment.environment.validation.validators.TagValidator;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Component
public class EnvironmentValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidatorService.class);

    private static final int ALL_ON_DEMAND_PERCENTAGE = 0;

    private static final int ALL_SPOT_PERCENTAGE = 100;

    private static final String DISALLOWED_CIDR = "0.0.0.0/0";

    private final NetworkValidator networkValidator;

    private final PlatformParameterService platformParameterService;

    private final EnvironmentResourceService environmentResourceService;

    private final CredentialService credentialService;

    private final PublicKeyValidator publicKeyValidator;

    private final Set<String> enabledParentPlatforms;

    private final Set<String> enabledChildPlatforms;

    private final TagValidator tagValidator;

    private final EncryptionKeyArnValidator encryptionKeyArnValidator;

    private final EncryptionKeyUrlValidator encryptionKeyUrlValidator;

    private final ManagedIdentityRoleValidator encryptionRoleValidator;

    private final EntitlementService entitlementService;

    private final EncryptionKeyValidator encryptionKeyValidator;

    private final EnvironmentRecipeService recipeService;

    private final Integer ipaMinimumInstanceCountByGroup;

    private final SeLinuxValidationService seLinuxValidationService;

    public EnvironmentValidatorService(NetworkValidator networkValidator,
            PlatformParameterService platformParameterService,
            EnvironmentResourceService environmentResourceService,
            CredentialService credentialService,
            PublicKeyValidator publicKeyValidator,
            @Value("${environment.enabledParentPlatforms}") Set<String> enabledParentPlatforms,
            @Value("${environment.enabledChildPlatforms}") Set<String> enabledChildPlatforms,
            TagValidator tagValidator,
            EncryptionKeyArnValidator encryptionKeyArnValidator,
            EncryptionKeyUrlValidator encryptionKeyUrlValidator,
            EntitlementService entitlementService,
            EncryptionKeyValidator encryptionKeyValidator,
            EnvironmentRecipeService recipeService,
            ManagedIdentityRoleValidator encryptionRoleValidator,
            @Value("${environment.freeipa.groupInstanceCount.minimum}") Integer ipaMinimumInstanceCountByGroup,
            SeLinuxValidationService seLinuxValidationService) {
        this.networkValidator = networkValidator;
        this.platformParameterService = platformParameterService;
        this.environmentResourceService = environmentResourceService;
        this.credentialService = credentialService;
        this.publicKeyValidator = publicKeyValidator;
        this.enabledChildPlatforms = enabledChildPlatforms;
        this.enabledParentPlatforms = enabledParentPlatforms;
        this.tagValidator = tagValidator;
        this.encryptionKeyArnValidator = encryptionKeyArnValidator;
        this.encryptionKeyUrlValidator = encryptionKeyUrlValidator;
        this.entitlementService = entitlementService;
        this.encryptionKeyValidator = encryptionKeyValidator;
        this.recipeService = recipeService;
        this.ipaMinimumInstanceCountByGroup = ipaMinimumInstanceCountByGroup;
        this.encryptionRoleValidator = encryptionRoleValidator;
        this.seLinuxValidationService = seLinuxValidationService;
    }

    public void validateFreeipaRecipesExistsByName(Set<String> resourceNames) {
        recipeService.validateFreeipaRecipesExistsByName(resourceNames);
    }

    public ValidationResultBuilder validateNetworkCreation(Environment environment, NetworkDto network) {
        return networkValidator.validateNetworkCreation(environment, network);
    }

    public ValidationResult validateTags(EnvironmentCreationDto environmentCreationDto) {
        return tagValidator.validateTags(environmentCreationDto.getCloudPlatform(), environmentCreationDto.getTags());
    }

    public ValidationResult validateTags(EnvironmentEditDto editDto) {
        return tagValidator.validateTags(editDto.getCloudPlatform(), editDto.getUserDefinedTags());
    }

    public ValidationResult validateParentChildRelation(Environment environment, String parentEnvironmentName) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        resultBuilder.ifError(() -> Objects.nonNull(parentEnvironmentName) && Objects.isNull(environment.getParentEnvironment()),
                String.format("Active parent environment with name '%s' is not available in account '%s'.", parentEnvironmentName, environment.getAccountId()));
        if (Objects.nonNull(environment.getParentEnvironment())) {
            resultBuilder.ifError(() -> environment.getParentEnvironment().getStatus() != EnvironmentStatus.AVAILABLE,
                    "Parent environment should be in 'AVAILABLE' status.");
            resultBuilder.ifError(() -> Objects.nonNull(environment.getParentEnvironment().getParentEnvironment()),
                    "Parent environment is already a child environment.");
            resultBuilder.ifError(() -> !platformEnabled(enabledChildPlatforms, environment.getCloudPlatform()),
                    String.format("'%s' platform is not supported for child environment.", environment.getCloudPlatform()));
            resultBuilder.ifError(() -> !platformEnabled(enabledParentPlatforms, environment.getParentEnvironment().getCloudPlatform()),
                    String.format("'%s' platform is not supported for parent environment.", environment.getParentEnvironment().getCloudPlatform()));
        }

        return resultBuilder.build();
    }

    public ValidationResult validateAwsEnvironmentRequest(EnvironmentRequest environmentRequest) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String cloudPlatform = credentialService.getCloudPlatformByCredential(environmentRequest.getCredentialName(), accountId, ENVIRONMENT);
        resultBuilder.ifError(() -> !AWS.name().equalsIgnoreCase(cloudPlatform),
                "Environment request is not for cloud platform AWS.");

        return resultBuilder.build();
    }

    public ValidationResult validateSecurityAccessModification(SecurityAccessDto securityAccessDto, Environment environment) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        resultBuilder.ifError(() -> isNotEmpty(securityAccessDto.getCidr()), "The CIDR could not be updated in the environment");
        resultBuilder.ifError(() -> isNotEmpty(environment.getCidr()) && isAnySecurityGroupMissing(securityAccessDto),
                "The CIDR can be replaced with the default and knox security groups, please add to the request");
        resultBuilder.ifError(() -> isAllSecurityGroupMissing(securityAccessDto),
                "Please add the default or knox security groups, we cannot edit with empty value.");
        return resultBuilder.build();
    }

    public ValidationResult validateSecurityGroups(EnvironmentEditDto editDto, Environment environment) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        getSecurityGroupIdSet(editDto).forEach(sg -> {
            try {
                fetchSecurityGroup(editDto, environment, sg);
            } catch (BadRequestException e) {
                LOGGER.info("Security group cannot be fetched, because BadRequest occurred with: " + e.getMessage());
                validationResultBuilder.error(e.getMessage());
            } catch (GetCloudParameterException e) {
                LOGGER.info("Security group cannot be fetched, because: " + e.getMessage());
                validationResultBuilder.error(e.getCause().getMessage());
            }
        });
        return validationResultBuilder.build();
    }

    public ValidationResult validateAuthenticationModification(EnvironmentEditDto editDto, Environment environment) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();

        AuthenticationDto authenticationDto = editDto.getAuthentication();
        String publicKeyId = authenticationDto.getPublicKeyId();
        Optional<PublicKeyConnector> publicKeyConnector = environmentResourceService.getPublicKeyConnector(environment.getCloudPlatform());
        if (publicKeyConnector.isEmpty() && isNotEmpty(publicKeyId)) {
            validationResultBuilder.error("The change of publicKeyId is not supported on " + environment.getCloudPlatform());
        } else {
            String publicKey = authenticationDto.getPublicKey();
            if (isNotEmpty(publicKeyId) && isNotEmpty(publicKey)) {
                validationResultBuilder.error("You should define either publicKey or publicKeyId only, but not both.");
            }
            if (StringUtils.isEmpty(publicKeyId) && StringUtils.isEmpty(publicKey)) {
                validationResultBuilder.error("You should define either the publicKey or the publicKeyId.");
            }
            if (isNotEmpty(publicKeyId) && !environmentResourceService.isPublicKeyIdExists(environment, publicKeyId)) {
                validationResultBuilder.error(String.format("The publicKeyId with name of '%s' does not exist on the provider.", publicKeyId));
            }
            if (isNotEmpty(publicKey)) {
                ValidationResult validationResult = publicKeyValidator.validatePublicKey(publicKey);
                validationResultBuilder.merge(validationResult);
            }
        }
        return validationResultBuilder.build();
    }

    public ValidationResult validatePublicKey(String publicKey) {
        return publicKeyValidator.validatePublicKey(publicKey);
    }

    private void fetchSecurityGroup(EnvironmentEditDto editDto, Environment environment, String securityGroupId) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                editDto.getAccountId(),
                environment.getCredential().getName(),
                null,
                environment.getRegionSet().stream().findFirst().get().getName(),
                environment.getCloudPlatform(),
                null);
        request.setFilters(Map.of("groupId", securityGroupId));
        platformParameterService.getSecurityGroups(request);
    }

    private Set<String> getSecurityGroupIdSet(EnvironmentEditDto editDto) {
        Set<String> groups = new HashSet<>();
        groups.addAll(getGroups(editDto.getSecurityAccess().getSecurityGroupIdForKnox()));
        groups.addAll(getGroups(editDto.getSecurityAccess().getDefaultSecurityGroupId()));
        return groups;
    }

    private Set<String> getGroups(String securityGroupIds) {
        return getSecurityGroupIds(securityGroupIds);
    }

    private boolean isAnySecurityGroupMissing(SecurityAccessDto securityAccessDto) {
        return StringUtils.isEmpty(securityAccessDto.getDefaultSecurityGroupId()) || StringUtils.isEmpty(securityAccessDto.getSecurityGroupIdForKnox());
    }

    private boolean isAllSecurityGroupMissing(SecurityAccessDto securityAccessDto) {
        return StringUtils.isEmpty(securityAccessDto.getDefaultSecurityGroupId()) && StringUtils.isEmpty(securityAccessDto.getSecurityGroupIdForKnox());
    }

    private boolean platformEnabled(Set<String> cloudPlatforms, String cloudPlatform) {
        return cloudPlatforms.stream().anyMatch(p -> p.equalsIgnoreCase(cloudPlatform));
    }

    public ValidationResult validateFreeIpaCreation(FreeIpaCreationDto freeIpaCreation, String accountId) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        int ipaInstanceCountByGroup = freeIpaCreation.getInstanceCountByGroup();
        if (ipaInstanceCountByGroup < ipaMinimumInstanceCountByGroup) {
            validationResultBuilder.error(
                    String.format("FreeIpa deployment requests are only allowed with at least '%d' instance(s) by group. The requested value was '%d'",
                            ipaMinimumInstanceCountByGroup, ipaInstanceCountByGroup));
        }
        if (ipaInstanceCountByGroup == 1 && !singleInstanceIsOnDemandOrSpot(freeIpaCreation)) {
            validationResultBuilder.error(
                    String.format("Single instance FreeIpa spot percentage must be either %d or %d.", ALL_ON_DEMAND_PERCENTAGE, ALL_SPOT_PERCENTAGE));
        }
        if (StringUtils.isNoneBlank(freeIpaCreation.getImageId(), freeIpaCreation.getImageOs())) {
            validationResultBuilder.error("FreeIpa deployment requests can not have both image id and image os parameters set.");
        }
        validateArchitecture(freeIpaCreation, accountId, validationResultBuilder);
        validateSeLinux(freeIpaCreation, validationResultBuilder);
        return validationResultBuilder.build();
    }

    private void validateArchitecture(FreeIpaCreationDto freeIpaCreation, String accountId, ValidationResultBuilder validationResultBuilder) {
        if (Architecture.ARM64.equals(freeIpaCreation.getArchitecture()) && !entitlementService.isDataLakeArmEnabled(accountId)) {
            validationResultBuilder.error("Your account is not entitled to use arm64 instances.");
        }
    }

    private void validateSeLinux(FreeIpaCreationDto freeIpaCreation, ValidationResultBuilder validationResultBuilder) {
        try {
            seLinuxValidationService.validateSeLinuxEntitlementGrantedForFreeipaCreation(freeIpaCreation);
        } catch (CloudbreakServiceException e) {
            validationResultBuilder.error(e.getMessage());
        }
    }

    private Boolean singleInstanceIsOnDemandOrSpot(FreeIpaCreationDto freeIpaCreation) {
        return Optional.ofNullable(freeIpaCreation.getAws())
                .map(FreeIpaCreationAwsParametersDto::getSpot)
                .map(FreeIpaCreationAwsSpotParametersDto::getPercentage)
                .map(spotPercentage -> spotPercentage == ALL_ON_DEMAND_PERCENTAGE || spotPercentage == ALL_SPOT_PERCENTAGE)
                .orElse(true);
    }

    public ValidationResult validateStorageLocation(String storageLocation, String storageType) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        if (storageLocation != null) {
            Pattern pattern = Pattern.compile("^[a-zA-Z0-9_/@:\\-.]+$");
            Matcher matcher = pattern.matcher(storageLocation.trim());
            if (!matcher.find()) {
                resultBuilder.error("You have added invalid characters to the storage location: " + storageLocation);
            }
        } else {
            String message = "You don't add a(n) %s storage location, please provide a valid storage location.";
            resultBuilder.error(String.format(message, storageType));
        }
        return resultBuilder.build();
    }

    public ValidationResult validateEncryptionKeyUrl(String encryptionKeyUrl) {
        return encryptionKeyUrlValidator.validateEncryptionKeyUrl(encryptionKeyUrl);
    }

    public ValidationResult validateEncryptionRole(String encryptionKeyRole) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        ValidationResult validationResult = encryptionRoleValidator.validateEncryptionRole(encryptionKeyRole);
        resultBuilder.merge(validationResult);
        return resultBuilder.build();
    }

    public ValidationResult validateEncryptionKeyArn(String encryptionKeyArn, boolean secretEncryptionEnabled) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        ValidationResult validationResult = encryptionKeyArnValidator.validateEncryptionKeyArn(encryptionKeyArn, secretEncryptionEnabled);
        resultBuilder.merge(validationResult);
        return resultBuilder.build();
    }

    public ValidationResult validateEncryptionKey(String encryptionKey) {
        return encryptionKeyValidator.validateEncryptionKey(encryptionKey);
    }

    public ValidationResult validateExternalizedComputeCluster(ExternalizedComputeClusterDto externalizedComputeCluster, String accountId,
            Set<String> environmentSubnets) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder().prefix("Default externalized compute cluster validation failed");
        if (externalizedComputeCluster.isCreate()) {
            validateWorkerNodeSubnets(externalizedComputeCluster, environmentSubnets, resultBuilder);
            if (externalizedComputeCluster.isPrivateCluster() && !externalizedComputeCluster.getKubeApiAuthorizedIpRanges().isEmpty()) {
                resultBuilder.error("The 'kubeApiAuthorizedIpRanges' parameter cannot be specified when 'privateCluster' is enabled.");
            }
            boolean internalTenant = entitlementService.internalTenant(accountId);
            if (!internalTenant) {
                if (!externalizedComputeCluster.isPrivateCluster() && externalizedComputeCluster.getKubeApiAuthorizedIpRanges().isEmpty()) {
                    resultBuilder.error("The 'kubeApiAuthorizedIpRanges' parameter must be specified when 'privateCluster' is disabled.");
                }
                if (externalizedComputeCluster.getKubeApiAuthorizedIpRanges().contains(DISALLOWED_CIDR)) {
                    resultBuilder.error(String.format("The value '%s' is not allowed for 'kubeApiAuthorizedIpRanges'.", DISALLOWED_CIDR));
                }
            }
        }
        return resultBuilder.build();
    }

    private void validateWorkerNodeSubnets(ExternalizedComputeClusterDto externalizedComputeCluster, Set<String> environmentSubnets,
            ValidationResultBuilder resultBuilder) {
        if (externalizedComputeCluster.getWorkerNodeSubnetIds() != null) {
            for (String workerNodeSubnet : externalizedComputeCluster.getWorkerNodeSubnetIds()) {
                if (!environmentSubnets.contains(workerNodeSubnet)) {
                    resultBuilder.error("Specified compute cluster subnet '" + workerNodeSubnet + "' does not exist in the environment");
                }
            }
        }
    }
}
