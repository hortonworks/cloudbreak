package com.sequenceiq.environment.environment.validation;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.logging.log4j.util.Strings.isEmpty;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentRegionValidator;
import com.sequenceiq.environment.environment.validation.validators.NetworkCreationValidator;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Component
public class EnvironmentValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidatorService.class);

    private static final int ALL_ON_DEMAND_PERCENTAGE = 0;

    private static final int ALL_SPOT_PERCENTAGE = 100;

    private final EnvironmentRegionValidator environmentRegionValidator;

    private final NetworkCreationValidator networkCreationValidator;

    private final PlatformParameterService platformParameterService;

    private final EnvironmentResourceService environmentResourceService;

    private final CredentialService credentialService;

    private Set<String> enabledParentPlatforms;

    private Set<String> enabledChildPlatforms;

    public EnvironmentValidatorService(EnvironmentRegionValidator environmentRegionValidator,
            NetworkCreationValidator networkCreationValidator,
            PlatformParameterService platformParameterService,
            EnvironmentResourceService environmentResourceService,
            CredentialService credentialService,
            @Value("${environment.enabledParentPlatforms}") Set<String> enabledParentPlatforms,
            @Value("${environment.enabledChildPlatforms}") Set<String> enabledChildPlatforms) {
        this.environmentRegionValidator = environmentRegionValidator;
        this.networkCreationValidator = networkCreationValidator;
        this.platformParameterService = platformParameterService;
        this.environmentResourceService = environmentResourceService;
        this.credentialService = credentialService;
        this.enabledChildPlatforms = enabledChildPlatforms;
        this.enabledParentPlatforms = enabledParentPlatforms;
    }

    public ValidationResultBuilder validateRegionsAndLocation(String location, Set<String> requestedRegions,
            Environment environment, CloudRegions cloudRegions) {
        String cloudPlatform = environment.getCloudPlatform();
        ValidationResultBuilder regionValidationResult
                = environmentRegionValidator.validateRegions(requestedRegions, cloudRegions, cloudPlatform);
        ValidationResultBuilder locationValidationResult
                = environmentRegionValidator.validateLocation(location, requestedRegions, cloudRegions, cloudPlatform);
        return regionValidationResult.merge(locationValidationResult.build());
    }

    public ValidationResultBuilder validateNetworkCreation(Environment environment, NetworkDto network) {
        return networkCreationValidator.validateNetworkCreation(environment, network);
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
        String cloudPlatform = credentialService.getCloudPlatformByCredential(environmentRequest.getCredentialName(), accountId);
        resultBuilder.ifError(() -> !AWS.name().equalsIgnoreCase(cloudPlatform),
                "Environment request is not for AWS.");

        resultBuilder.ifError(() -> StringUtils.isBlank(Optional.ofNullable(environmentRequest.getAws())
                .map(AwsEnvironmentParameters::getS3guard)
                .map(S3GuardRequestParameters::getDynamoDbTableName)
                .orElse(null)), "S3Guard Dynamo DB table name is not found in environment request.");
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
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();

        AuthenticationDto authenticationDto = editDto.getAuthentication();
        String publicKeyId = authenticationDto.getPublicKeyId();
        Optional<PublicKeyConnector> publicKeyConnector = environmentResourceService.getPublicKeyConnector(environment.getCloudPlatform());
        if (publicKeyConnector.isEmpty() && StringUtils.isNotEmpty(publicKeyId)) {
            validationResultBuilder.error("The change of publicKeyId is not supported on " + environment.getCloudPlatform());
        } else {
            String publicKey = authenticationDto.getPublicKey();
            if (StringUtils.isNotEmpty(publicKeyId) && StringUtils.isNotEmpty(publicKey)) {
                validationResultBuilder.error("You should define either publicKey or publicKeyId only");
            }
            if (StringUtils.isEmpty(publicKeyId) && StringUtils.isEmpty(publicKey)) {
                validationResultBuilder.error("You should define publicKey or publicKeyId");
            }
            if (StringUtils.isNotEmpty(publicKeyId) && !environmentResourceService.isPublicKeyIdExists(environment, publicKeyId)) {
                validationResultBuilder.error(String.format("The publicKeyId with name of '%s' does not exists on the provider", publicKeyId));
            }
        }
        return validationResultBuilder.build();
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
        return Set.of(editDto.getSecurityAccess().getSecurityGroupIdForKnox(), editDto.getSecurityAccess().getDefaultSecurityGroupId());
    }

    private boolean isAnySecurityGroupMissing(SecurityAccessDto securityAccessDto) {
        return isEmpty(securityAccessDto.getDefaultSecurityGroupId()) || isEmpty(securityAccessDto.getSecurityGroupIdForKnox());
    }

    private boolean isAllSecurityGroupMissing(SecurityAccessDto securityAccessDto) {
        return isEmpty(securityAccessDto.getDefaultSecurityGroupId()) && isEmpty(securityAccessDto.getSecurityGroupIdForKnox());
    }

    private boolean platformEnabled(Set<String> cloudPlatforms, String cloudPlatform) {
        return cloudPlatforms.stream().anyMatch(p -> p.equalsIgnoreCase(cloudPlatform));
    }

    public ValidationResult validateFreeIpaCreation(FreeIpaCreationDto freeIpaCreation) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (freeIpaCreation.getInstanceCountByGroup() == 1 && !singleInstanceIsOnDemandOrSpot(freeIpaCreation)) {
            validationResultBuilder.error(
                    String.format("Single instance FreeIpa spot percentage must be either %d or %d.", ALL_ON_DEMAND_PERCENTAGE, ALL_SPOT_PERCENTAGE));
        }
        return validationResultBuilder.build();
    }

    private Boolean singleInstanceIsOnDemandOrSpot(FreeIpaCreationDto freeIpaCreation) {
        return Optional.ofNullable(freeIpaCreation.getAws())
                .map(FreeIpaCreationAwsParametersDto::getSpot)
                .map(FreeIpaCreationAwsSpotParametersDto::getPercentage)
                .map(spotPercentage -> spotPercentage == ALL_ON_DEMAND_PERCENTAGE || spotPercentage == ALL_SPOT_PERCENTAGE)
                .orElse(true);
    }
}
