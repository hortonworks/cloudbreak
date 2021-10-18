package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;

import com.amazonaws.AmazonServiceException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
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

    @BeforeEach
    void setUp() {
        underTest = new EncryptionKeyArnValidator(credentialToCloudCredentialConverter, retryService, cloudPlatformConnectors);
    }

    @Test
    void testEncryptionKeyArnValidationWithValidKeyAndCommentIsValid() {
        String validKey = "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";

        ValidationResult validationResult = underTest.validateEncryptionKeyArn(validKey);
        assertFalse(validationResult.hasError());

    }

    @Test
    void testEncryptionKeyUrlValidationWithInValidKeyAndCommentIsValid() {
        String invalidKey = "aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";

        ValidationResult validationResult = underTest.validateEncryptionKeyArn(invalidKey);
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
        String invalidKey =  "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";
        EnvironmentDto environmentDto = createEnvironmentDto(invalidKey);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        CloudEncryptionKey testInput = new CloudEncryptionKey();
        testInput.setName("arn:aws:kms:eu-west-2:123456789012:key/1a2b3c4d-5e6f-7g8h-9i0j-1k2l3m4n5o6p");
        CloudEncryptionKeys cloudEncryptionKeys = new CloudEncryptionKeys(Set.of(testInput));
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenReturn(cloudEncryptionKeys);

        ValidationResult validationResult = underTest.validate(environmentValidationDto);
        assertTrue(validationResult.hasError());
        assertEquals(String.format("The provided encryption key does not exist in the given region's encryption key list for this credential."),
                validationResult.getFormattedErrors());
    }

    @Test
    void testWithValidEncryptionKeyWithAwsListKeysCall() {
        String validKey =  "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";
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
        String validKey =  "arn:aws:kms:us-east-1:012345678910:key/1234abcd-12ab-34cd-56ef-1234567890ab";
        EnvironmentDto environmentDto = createEnvironmentDto(validKey);
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder().withEnvironmentDto(environmentDto).build();
        AmazonServiceException amazonServiceException = new AmazonServiceException("An unexpected error occurred while trying to fetch the KMS keys from AWS");
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenThrow(amazonServiceException);
        assertThrows(AmazonServiceException.class, () -> underTest.validate(environmentValidationDto));
    }

    @Test
    public void testWithEmptyEncryptionKeyAndEmptyParameters() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withLocationDto(LocationDto.builder()
                        .withName(REGION).build())
                .withCredential(credential)
                .withCloudPlatform("AWS")
                .withParameters(ParametersDto.builder()
                        .withAwsParameters(null)
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

    private EnvironmentDto createEnvironmentDto(String encryptionKeyArn) {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withLocationDto(LocationDto.builder()
                        .withName(REGION).build())
                .withCredential(credential)
                .withCloudPlatform("AWS")
                .withParameters(ParametersDto.builder()
                        .withAwsParameters(AwsParametersDto.builder()
                                .withAwsDiskEncryptionParameters(AwsDiskEncryptionParametersDto.builder()
                                        .withEncryptionKeyArn(encryptionKeyArn).build())
                                .build())
                        .build())
                .build();
        return environmentDto;
    }
}