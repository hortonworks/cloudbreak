package com.sequenceiq.environment.environment.validation.validators;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

@Component
public class EncryptionKeyArnValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionKeyArnValidator.class);

    private static final Pattern ENCRYPTION_KEY_ARN_PATTERN = Pattern.compile("^arn:(aws|aws-cn|aws-us-gov):kms:[a-zA-Z0-9-]+:[0-9]+:" +
            "key/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Qualifier("DefaultRetryService")
    private Retry retryService;

    private CloudPlatformConnectors cloudPlatformConnectors;

    private EntitlementService entitlementService;

    public EncryptionKeyArnValidator(
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            Retry retryService,
            CloudPlatformConnectors cloudPlatformConnectors,
            EntitlementService entitlementService) {
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.retryService = retryService;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.entitlementService = entitlementService;
    }

    public ValidationResult validateEncryptionKeyArn(String encryptionKeyArn) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        Matcher matcher = ENCRYPTION_KEY_ARN_PATTERN.matcher(encryptionKeyArn.trim());
        if (!matcher.matches()) {
            validationResultBuilder.error(String.format("The identifier of the AWS Key Management Service (AWS KMS)" +
                    "customer master key (CMK) to use for Amazon EBS encryption.%n" +
                    "You can specify the key ARN in the below format:%n" +
                    "Key ARN: arn:partition:service:region:account-id:resource-type/resource-id. " +
                    "For example, arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab.%n"));
        }
        return validationResultBuilder.build();
    }

    public ValidationResult validate(EnvironmentValidationDto environmentValidationDto) {
        String encryptionKeyArn = Optional.ofNullable(environmentValidationDto)
                .map(EnvironmentValidationDto::getEnvironmentDto)
                .map(environmentDto -> environmentDto.getParameters())
                .map(ParametersDto::getAwsParametersDto)
                .map(AwsParametersDto::getAwsDiskEncryptionParametersDto)
                .map(AwsDiskEncryptionParametersDto::getEncryptionKeyArn)
                .orElse(null);
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (encryptionKeyArn == null || encryptionKeyArn.isEmpty()) {
            return validationResultBuilder.build();
        }

        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environmentDto.getCredential());
        ExtendedCloudCredential extendedCloudCredential = new ExtendedCloudCredential(
                cloudCredential,
                environmentDto.getCloudPlatform(),
                environmentDto.getDescription(),
                environmentDto.getCreator(),
                environmentDto.getAccountId(),
                entitlementService.getEntitlements(environmentDto.getAccountId()));
        Region region = region(environmentDto.getLocation().getName());
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(environmentDto.getCloudPlatform()), null);

        try {
            CloudEncryptionKeys encryptionKeys =  retryService.testWith2SecDelayMax15Times(() -> cloudPlatformConnectors.get(cloudPlatformVariant).
                    platformResources().encryptionKeys(extendedCloudCredential, region, Collections.emptyMap()));
            List<String> keyArns = encryptionKeys.getCloudEncryptionKeys().stream().map(CloudEncryptionKey::getName).collect(Collectors.toList());
            if (keyArns.stream().noneMatch(s -> s.equals(encryptionKeyArn))) {
                validationResultBuilder.warning("Following encryption keys are retrieved from the cloud " + keyArns +
                        " . The provided encryption key " + encryptionKeyArn +
                        " does not exist in the given region's encryption key list for this credential." +
                        " This is possible if the key is present in a different AWS Account." +
                        " Please ensure that the Key is present and have valid permissions otherwise it would result in failures with EBS volume creation.");


            }
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred while trying to fetch the KMS keys from AWS");
            throw e;
        }
        return validationResultBuilder.build();
    }
}