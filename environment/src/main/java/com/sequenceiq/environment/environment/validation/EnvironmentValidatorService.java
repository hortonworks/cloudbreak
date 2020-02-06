package com.sequenceiq.environment.environment.validation;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.logging.log4j.util.Strings.isEmpty;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
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

    private final EnvironmentRegionValidator environmentRegionValidator;

    private final NetworkCreationValidator networkCreationValidator;

    private final PlatformParameterService platformParameterService;

    private final EnvironmentResourceService environmentResourceService;

    public EnvironmentValidatorService(EnvironmentRegionValidator environmentRegionValidator, NetworkCreationValidator networkCreationValidator,
            PlatformParameterService platformParameterService, EnvironmentResourceService environmentResourceService) {
        this.environmentRegionValidator = environmentRegionValidator;
        this.networkCreationValidator = networkCreationValidator;
        this.platformParameterService = platformParameterService;
        this.environmentResourceService = environmentResourceService;
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

    public ValidationResultBuilder validateNetworkCreation(Environment environment, NetworkDto network, Map<String, CloudSubnet> subnetMetas) {
        return networkCreationValidator.validateNetworkCreation(environment, network, subnetMetas);
    }

    public ValidationResult validateAwsEnvironmentRequest(EnvironmentRequest environmentRequest, String cloudPlatform) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        resultBuilder.ifError(() -> !CloudPlatform.AWS.name().equalsIgnoreCase(cloudPlatform),
                "Environment request is not for AWS.");

        resultBuilder.ifError(() -> StringUtils.isBlank(Optional.ofNullable(environmentRequest.getAws())
                .map(AwsEnvironmentParameters::getS3guard)
                .map(S3GuardRequestParameters::getDynamoDbTableName)
                .orElse(null)), "S3Guard Dynamo DB table name is not found in environment request.");
        return resultBuilder.build();
    }

    public ValidationResult validateAwsEnvironmentRequest(EnvironmentDto environmentDto) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        resultBuilder.ifError(() -> !AWS.name().equalsIgnoreCase(environmentDto.getCloudPlatform()),
                "Environment is not in AWS.");
        return resultBuilder.build();
    }

    public ValidationResult validateSecurityAccessModification(SecurityAccessDto securityAccessDto, Environment environment) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        resultBuilder.ifError(() -> isNotEmpty(securityAccessDto.getCidr()), "The CIDR could not be updated in the environment");
        resultBuilder.ifError(() -> isNotEmpty(environment.getCidr()) && anySecGroupMissing(securityAccessDto),
                "The CIDR can be replaced with the default and knox security groups, please add to the request");
        resultBuilder.ifError(() -> allSecGroupMissing(securityAccessDto),
                "Please add the default or knox security groups, we cannot edit with empty value.");
        return resultBuilder.build();
    }

    public ValidationResult validateSecurityGroups(EnvironmentEditDto editDto, Environment environment) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
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

    private CloudSecurityGroups fetchSecurityGroup(EnvironmentEditDto editDto, Environment environment, String securityGroupId) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                editDto.getAccountId(),
                environment.getCredential().getName(),
                null,
                environment.getRegionSet().stream().findFirst().get().getName(),
                environment.getCloudPlatform(),
                null);
        request.setFilters(Map.of("groupId", securityGroupId));
        return platformParameterService.getSecurityGroups(request);
    }

    private Set<String> getSecurityGroupIdSet(EnvironmentEditDto editDto) {
        return Set.of(editDto.getSecurityAccess().getSecurityGroupIdForKnox(), editDto.getSecurityAccess().getDefaultSecurityGroupId());
    }

    private boolean anySecGroupMissing(SecurityAccessDto securityAccessDto) {
        return isEmpty(securityAccessDto.getDefaultSecurityGroupId()) || isEmpty(securityAccessDto.getSecurityGroupIdForKnox());
    }

    private boolean allSecGroupMissing(SecurityAccessDto securityAccessDto) {
        return isEmpty(securityAccessDto.getDefaultSecurityGroupId()) && isEmpty(securityAccessDto.getSecurityGroupIdForKnox());
    }
}
