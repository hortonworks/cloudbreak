package com.sequenceiq.environment.environment.validation.validators;

import static com.sequenceiq.environment.environment.validation.validators.EncryptionKeyArnValidator.NULL_ARN_WITH_SECRET_ENCRYPTION_ENABLED_ERROR_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.AwsConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

@ExtendWith(MockitoExtension.class)
class EncryptionKeyArnValidatorTest {

    private static final String REGION = "dummyRegion";

    private EncryptionKeyArnValidator underTest;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Mock
    private Credential credential;

    @Mock
    private Region region;

    @InjectMocks
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private EntitlementService entitlementService;

    @BeforeEach
    void setUp() {
        underTest = new EncryptionKeyArnValidator(credentialToCloudCredentialConverter, retryService, cloudPlatformConnectors, entitlementService);
    }

    @Test
    void testEncryptionKeyArnValidationWithValidKeyAndCommentIsValid() {
        String validKey = "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";

        ValidationResult validationResult = underTest.validateEncryptionKeyArn(validKey, false);
        assertFalse(validationResult.hasError());

    }

    @Test
    void testEncryptionKeyUrlValidationWithInValidKeyAndCommentIsValid() {
        String invalidKey = "aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";

        ValidationResult validationResult = underTest.validateEncryptionKeyArn(invalidKey, false);
        assertTrue(validationResult.hasError());
        assertEquals(String.format("The identifier of the AWS Key Management Service (AWS KMS)" +
                        "customer master key (CMK) to use for Amazon EBS encryption.%n" +
                        "You can specify the key ARN in the below format:%n" +
                        "Key ARN: arn:partition:service:region:account-id:resource-type/resource-id. " +
                        "For example, arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab.%n"),
                validationResult.getFormattedErrors());
    }

    @Test
    void testWithInvalidEncryptionKeyWithAwsListKeysCall() {
        String invalidKey = "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";
        EnvironmentDto environmentDto = createEnvironmentDto(invalidKey);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        CloudEncryptionKey testInput = new CloudEncryptionKey();
        testInput.setName("arn:aws:kms:eu-west-2:123456789012:key/1a2b3c4d-5e6f-7g8h-9i0j-1k2l3m4n5o6p");
        CloudEncryptionKey testInput1 = new CloudEncryptionKey();
        testInput1.setName("arn:aws:kms:eu-west-2:123456789012:key/1a2b3c4d-5e6f-jjjj-9i0j-1k2l3m4n5o6p");
        CloudEncryptionKeys cloudEncryptionKeys = new CloudEncryptionKeys(Set.of(testInput, testInput1));
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenReturn(cloudEncryptionKeys);
        ValidationResult validationResult = underTest.validate(environmentValidationDto);
        assertTrue(validationResult.hasWarning());

        assertEquals(String.format("Following encryption keys are retrieved from the cloud %s ." +
                " The provided encryption key %s does not exist in the given region's encryption key list for this credential." +
                " This is possible if the key is present in a different AWS Account." +
                " Please ensure that the Key is present and have valid permissions otherwise it would result in failures with EBS volume creation.",
                cloudEncryptionKeys.getCloudEncryptionKeys().stream().map(CloudEncryptionKey::getName).collect(Collectors.toList()),
                invalidKey),
                validationResult.getFormattedWarnings());
    }

    @Test
    void testWithValidEncryptionKeyWithAwsListKeysCall() {
        String validKey = "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";
        EnvironmentDto environmentDto = createEnvironmentDto(validKey);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        CloudEncryptionKey testInput = new CloudEncryptionKey();
        testInput.setName("arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab");
        CloudEncryptionKeys cloudEncryptionKeys = new CloudEncryptionKeys(Set.of(testInput));
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenReturn(cloudEncryptionKeys);

        ValidationResult validationResult = underTest.validate(environmentValidationDto);
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWithValidEncryptionKeyAndAwsListKeysCallFailed() {
        String validKey = "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";
        EnvironmentDto environmentDto = createEnvironmentDto(validKey);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message("An unexpected error occurred while trying to fetch the KMS keys from AWS").build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenThrow(amazonServiceException);
        assertThrows(AwsServiceException.class, () -> underTest.validate(environmentValidationDto));
    }

    @Test
    public void testWithEmptyEncryptionKeyAndEmptyParameters() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withLocationDto(LocationDto.builder()
                        .withName(REGION).build())
                .withCredential(credential)
                .withCloudPlatform("AWS")
                .withParameters(ParametersDto.builder()
                        .withAwsParametersDto(null)
                        .build())
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        ValidationResult validationResult = underTest.validate(environmentValidationDto);
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWithEmptyEnvironmentDto() {
        EnvironmentDto environmentDto = EnvironmentDto.builder().build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        ValidationResult validationResult = underTest.validate(environmentValidationDto);
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testWithEmptyEncryptionKeyAndSecretEncryptionEnabled() {
        ValidationResult validationResult = underTest.validateEncryptionKeyArn(null, true);
        assertEquals(NULL_ARN_WITH_SECRET_ENCRYPTION_ENABLED_ERROR_MESSAGE, validationResult.getFormattedErrors());
    }

    @Test
    public void testInvalidEncryptionKeyArnFormatWithValideAccess() {
        String invalidKey = "arn:aws:kms:us-east-1:012345678910";
        EnvironmentDto environmentDto = createEnvironmentDto(invalidKey);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message("An unexpected error occurred while trying to fetch the KMS keys from AWS").build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);

        ValidationResult result = underTest.validate(environmentValidationDto, true);
        assertEquals(String.format("The key ARN is in bad format. " +
                        "You can specify the key ARN in the below format: " +
                        "Key ARN: arn:partition:service:region:account-id:resource-type/resource-id. " +
                        "For example, arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab."),
                result.getFormattedWarnings());
    }

    @Test
    public void testInvalidEncryptionKeyArnDontValidateAccess() {
        String validKey = "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";
        EnvironmentDto environmentDto = createEnvironmentDto(validKey);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message("An unexpected error occurred while trying to fetch the KMS keys from AWS").build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);

        CloudPlatformConnectors cloudPlatformConnectors = mock(CloudPlatformConnectors.class);
        AwsConnector connector = mock(AwsConnector.class);
        PlatformResources platformResources = mock(PlatformResources.class);

        //CloudPlatformVariant cloudPlatformVariant = mock(CloudPlatformVariant.class);
        ExtendedCloudCredential extendedCloudCredential = mock(ExtendedCloudCredential.class);

        CloudEncryptionKeys encryptionKeys = mock(CloudEncryptionKeys.class);
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenReturn(encryptionKeys);

        List<CloudEncryptionKey> cloudEncryptionKeysList = new ArrayList<>();
        when(encryptionKeys.getCloudEncryptionKeys()).thenReturn(cloudEncryptionKeysList);

        ValidationResult result = underTest.validate(environmentValidationDto, false);

        List<String> keyArns = new ArrayList<>();
        keyArns.add(validKey);

        assertEquals("Following encryption keys are retrieved from the cloud [] ." +
                        " The provided encryption key arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab" +
                        " does not exist in the given region's encryption key list for this credential." +
                        " This is possible if the key is present in a different AWS Account." +
                        " Please ensure that the Key is present and have valid permissions otherwise it would result in failures with EBS volume creation.",
                result.getFormattedWarnings());
    }

    @Test
    public void testValidEncryptionKeyArnDontValidateAccess() {
        String validKey = "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";
        EnvironmentDto environmentDto = createEnvironmentDto(validKey);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message("An unexpected error occurred while trying to fetch the KMS keys from AWS").build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);

        CloudPlatformConnectors cloudPlatformConnectors = mock(CloudPlatformConnectors.class);
        AwsConnector connector = mock(AwsConnector.class);
        PlatformResources platformResources = mock(PlatformResources.class);


        // Define input parameters
        CloudPlatformVariant cloudPlatformVariant = mock(CloudPlatformVariant.class);
        ExtendedCloudCredential extendedCloudCredential = mock(ExtendedCloudCredential.class);

        String region = REGION;

        CloudEncryptionKeys encryptionKeys = mock(CloudEncryptionKeys.class);
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenReturn(encryptionKeys);


        List<CloudEncryptionKey> cloudEncryptionKeysList = new ArrayList<>();
        CloudEncryptionKey cloudEncryptionKey = new CloudEncryptionKey(validKey, "key-id", "", "", null);
        cloudEncryptionKeysList.add(cloudEncryptionKey);

        when(encryptionKeys.getCloudEncryptionKeys()).thenReturn(cloudEncryptionKeysList);


        ValidationResult result = underTest.validate(environmentValidationDto, false);

        List<String> keyArns = new ArrayList<>();
        keyArns.add(validKey);

        assertEquals("", result.getFormattedWarnings());
    }

    @Test
    public void testEncryptionKeyWithInValideAccess() {
        String validKey = "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";
        EnvironmentDto environmentDto = createEnvironmentDto(validKey);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message("An unexpected error occurred while trying to fetch the KMS keys from AWS").build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenReturn(false);
        ValidationResult result = underTest.validate(environmentValidationDto, true);
        assertEquals(String.format("The provided encryption key " + validKey + " is not usable." +
                " This is possible if the key is present in a different AWS Account." +
                " Please ensure that the Key is present and have valid permissions otherwise it would result in failures with EBS volume creation."),
                result.getFormattedWarnings());
    }

    @Test
    public void testEncryptionKeyWithValideAccess() {
        String validKey = "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";
        EnvironmentDto environmentDto = createEnvironmentDto(validKey);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message("An unexpected error occurred while trying to fetch the KMS keys from AWS").build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenReturn(true);
        ValidationResult result = underTest.validate(environmentValidationDto, true);
        assertEquals("", result.getFormattedWarnings());
    }

    private EnvironmentDto createEnvironmentDto(String encryptionKeyArn) {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withLocationDto(LocationDto.builder()
                        .withName(REGION).build())
                .withCredential(credential)
                .withCloudPlatform("AWS")
                .withParameters(ParametersDto.builder()
                        .withAwsParametersDto(AwsParametersDto.builder()
                                .withAwsDiskEncryptionParametersDto(AwsDiskEncryptionParametersDto.builder()
                                        .withEncryptionKeyArn(encryptionKeyArn).build())
                                .build())
                        .build())
                .build();
        return environmentDto;
    }
}
