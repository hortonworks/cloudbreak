package com.sequenceiq.environment.environment.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.validation.cloudstorage.EnvironmentLogStorageLocationValidator;
import com.sequenceiq.environment.environment.validation.validators.EnvironmentRegionValidator;
import com.sequenceiq.environment.parameters.validation.validators.AwsParameterProcessor;

@ExtendWith(MockitoExtension.class)
class EnvironmentValidatorServiceTest {

    @Mock
    private EnvironmentRegionValidator regionValidator;

    @Mock
    private EnvironmentLogStorageLocationValidator logStorageLocationValidator;

    @Mock
    private AwsParameterProcessor awsParameterProcessor;

    @Mock
    private EnvironmentResourceService environmentResourceService;

    @InjectMocks
    private EnvironmentValidatorService underTest;

    @Test
    void testValidateAwsEnvironmentDtoNotAWS() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setCloudPlatform("AZURE");
        ValidationResult result = underTest.validateAwsEnvironmentRequest(environmentDto);
        assertTrue(result.hasError());
        assertEquals("Environment is not in AWS.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentDtoValid() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setCloudPlatform("AWS");
        ValidationResult result = underTest.validateAwsEnvironmentRequest(environmentDto);
        assertFalse(result.hasError());
    }

    @Test
    void testValidateAwsEnvironmentRequestNotAWS() {
        EnvironmentRequest request = new EnvironmentRequest();
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request, "AZURE");
        assertTrue(result.hasError());
        assertEquals("Environment request is not for AWS.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestNoAwsParams() {
        EnvironmentRequest request = new EnvironmentRequest();
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request, "AWS");
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentNoS3GuardParams() {
        EnvironmentRequest request = new EnvironmentRequest();
        request.setAws(new AwsEnvironmentParameters());
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request, "AWS");
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestNoDynamoTable() {
        EnvironmentRequest request = new EnvironmentRequest();
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        aws.setS3guard(new S3GuardRequestParameters());
        request.setAws(aws);
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request, "AWS");
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestValid() {
        EnvironmentRequest request = new EnvironmentRequest();
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        S3GuardRequestParameters s3GuardRequestParameters = new S3GuardRequestParameters();
        s3GuardRequestParameters.setDynamoDbTableName("table");
        aws.setS3guard(s3GuardRequestParameters);
        request.setAws(aws);
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request, "AWS");
        assertFalse(result.hasError());
    }

    @Test
    void testValidateSecurityAccessModificationWhenDefaultSecGroupAdded() {
        Environment environment = new Environment();
        SecurityAccessDto securityAccessDto = SecurityAccessDto.builder()
                .withDefaultSecurityGroupId("sec-group")
                .build();
        ValidationResult validationResult = underTest.validateSecurityAccessModification(securityAccessDto, environment);

        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateSecurityAccessModificationWhenKnoxSecGroupAdded() {
        Environment environment = new Environment();
        SecurityAccessDto securityAccessDto = SecurityAccessDto.builder()
                .withSecurityGroupIdForKnox("knox-sec-group")
                .build();
        ValidationResult validationResult = underTest.validateSecurityAccessModification(securityAccessDto, environment);

        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateSecurityAccessModificationWhenEnvCidrIsNotEmptyButDefaultSecGroupAddedOnly() {
        Environment environment = new Environment();
        environment.setCidr("cidr");
        SecurityAccessDto securityAccessDto = SecurityAccessDto.builder()
                .withDefaultSecurityGroupId("sec-group")
                .build();
        ValidationResult validationResult = underTest.validateSecurityAccessModification(securityAccessDto, environment);

        assertTrue(validationResult.hasError());
        assertEquals("1. The CIDR can be replaced with the default and knox security groups, please add to the request", validationResult.getFormattedErrors());
    }

    @Test
    void testValidateSecurityAccessModificationWhenEnvCidrIsNotEmptyAndKnoxAndDefaultSecGroupAdded() {
        Environment environment = new Environment();
        environment.setCidr("cidr");
        SecurityAccessDto securityAccessDto = SecurityAccessDto.builder()
                .withDefaultSecurityGroupId("sec-group")
                .withSecurityGroupIdForKnox("knox-sec-group")
                .build();
        ValidationResult validationResult = underTest.validateSecurityAccessModification(securityAccessDto, environment);

        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateSecurityAccessModificationWhenCidrAddedOnlyInRequest() {
        Environment environment = new Environment();
        SecurityAccessDto securityAccessDto = SecurityAccessDto.builder()
                .withCidr("cidr")
                .build();
        ValidationResult validationResult = underTest.validateSecurityAccessModification(securityAccessDto, environment);

        assertTrue(validationResult.hasError());
        assertEquals("1. Please add the default or knox security groups, we cannot edit with empty value.\n" +
                "2. The CIDR could not be updated in the environment", validationResult.getFormattedErrors());
    }

    @Test
    void testValidateSecurityAccessModificationWhenCidrAndDefaultSecurityGroupAddedInRequest() {
        Environment environment = new Environment();
        SecurityAccessDto securityAccessDto = SecurityAccessDto.builder()
                .withCidr("cidr")
                .withDefaultSecurityGroupId("sec-group")
                .build();
        ValidationResult validationResult = underTest.validateSecurityAccessModification(securityAccessDto, environment);

        assertTrue(validationResult.hasError());
        assertEquals("1. The CIDR could not be updated in the environment", validationResult.getFormattedErrors());
    }

    @Test
    void testValidateAuthenticationModificationWhenNotAwsAndHasPublicKeyId() {
        Environment environment = new Environment();
        environment.setCloudPlatform("AZURE");
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder()
                .withAuthentication(AuthenticationDto.builder()
                        .withPublicKeyId("pub-key-id")
                        .build())
                .build();

        when(environmentResourceService.getPublicKeyConnector(environment.getCloudPlatform())).thenReturn(Optional.empty());

        ValidationResult validationResult = underTest.validateAuthenticationModification(environmentEditDto, environment);
        assertEquals("1. The change of publicKeyId is not supported on AZURE", validationResult.getFormattedErrors());
    }

    @Test
    void testValidateAuthenticationModificationWhenHasPublicKeyAndPublicKeyIdAsWell() {
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder()
                .withAuthentication(AuthenticationDto.builder()
                        .withPublicKeyId("pub-key-id")
                        .withPublicKey("ssh-key")
                        .build())
                .build();
        PublicKeyConnector connector = mock(PublicKeyConnector.class);

        when(environmentResourceService.isPublicKeyIdExists(environment, "pub-key-id")).thenReturn(true);
        when(environmentResourceService.getPublicKeyConnector(environment.getCloudPlatform())).thenReturn(Optional.of(connector));

        ValidationResult validationResult = underTest.validateAuthenticationModification(environmentEditDto, environment);
        assertEquals("1. You should define either publicKey or publicKeyId only", validationResult.getFormattedErrors());
    }

    @Test
    void testValidateAuthenticationModificationWhenPublicKeyAndPublicKeyIdIsEmptyAsWell() {
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder()
                .withAuthentication(AuthenticationDto.builder().build())
                .build();

        ValidationResult validationResult = underTest.validateAuthenticationModification(environmentEditDto, environment);
        assertEquals("1. You should define publicKey or publicKeyId", validationResult.getFormattedErrors());
    }

    @Test
    void testValidateAuthenticationModificationWhenHasPublicKeyIdButNotExists() {
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder()
                .withAuthentication(AuthenticationDto.builder()
                        .withPublicKeyId("pub-key-id")
                        .build())
                .build();

        PublicKeyConnector connector = mock(PublicKeyConnector.class);
        when(environmentResourceService.isPublicKeyIdExists(environment, "pub-key-id")).thenReturn(false);
        when(environmentResourceService.getPublicKeyConnector(environment.getCloudPlatform())).thenReturn(Optional.of(connector));


        ValidationResult validationResult = underTest.validateAuthenticationModification(environmentEditDto, environment);
        assertEquals("1. The publicKeyId with name of 'pub-key-id' does not exists on the provider", validationResult.getFormattedErrors());
    }
}
