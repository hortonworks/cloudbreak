package com.sequenceiq.environment.environment.validation;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
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
import com.sequenceiq.environment.platformresource.PlatformParameterService;

@ExtendWith(MockitoExtension.class)
class EnvironmentValidatorServiceTest {

    private static final String ACCOUNT = "account";

    @Mock
    private EnvironmentRegionValidator environmentRegionValidator;

    @Mock
    private NetworkCreationValidator networkCreationValidator;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private EnvironmentResourceService environmentResourceService;

    @InjectMocks
    private EnvironmentValidatorService underTest;

    @BeforeEach
    public void initTests() {
        underTest = new EnvironmentValidatorService(
                environmentRegionValidator,
                networkCreationValidator,
                platformParameterService,
                environmentResourceService,
                singleton(CloudPlatform.AWS.name()),
                singleton(CloudPlatform.YARN.name())
        );
    }

    @Test
    void testValidateAwsEnvironmentRequestNotAWS() {
        EnvironmentRequest request = new EnvironmentRequest();
        request.setCloudPlatform("AZURE");
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request);
        assertTrue(result.hasError());
        assertEquals("Environment request is not for AWS.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestNoAwsParams() {
        EnvironmentRequest request = new EnvironmentRequest();
        request.setCloudPlatform("AWS");
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request);
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentNoS3GuardParams() {
        EnvironmentRequest request = new EnvironmentRequest();
        request.setCloudPlatform("AWS");
        request.setAws(new AwsEnvironmentParameters());
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request);
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestNoDynamoTable() {
        EnvironmentRequest request = new EnvironmentRequest();
        request.setCloudPlatform("AWS");
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        aws.setS3guard(new S3GuardRequestParameters());
        request.setAws(aws);
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request);
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestValid() {
        EnvironmentRequest request = new EnvironmentRequest();
        request.setCloudPlatform("AWS");
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        S3GuardRequestParameters s3GuardRequestParameters = new S3GuardRequestParameters();
        s3GuardRequestParameters.setDynamoDbTableName("table");
        aws.setS3guard(s3GuardRequestParameters);
        request.setAws(aws);
        ValidationResult result = underTest.validateAwsEnvironmentRequest(request);
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

    @Test
    void shouldNotFailInCaseOfVaildParentChildEnvironment() {
        Environment environment = aValidEnvirontmentWithParent();

        ValidationResult validationResult = underTest.validateParentChildRelation(environment, "parentEnvName");
        assertFalse(validationResult.hasError());
    }

    @Test
    void shouldFailOnExistingParentEnvironmentNameButMissingParentEntity() {
        Environment environment = aValidEnvirontmentWithParent();
        environment.setParentEnvironment(null);

        ValidationResult validationResult = underTest.validateParentChildRelation(environment, "parentEnvName");
        assertEquals("1. Active parent environment with name 'parentEnvName' is not available in account '" + ACCOUNT + "'.",
                validationResult.getFormattedErrors());
    }

    @Test
    void shouldFailOnParentEnvironmentInNonActiveState() {
        Environment environment = aValidEnvirontmentWithParent();
        environment.getParentEnvironment().setStatus(EnvironmentStatus.ARCHIVED);
        ValidationResult validationResult = underTest.validateParentChildRelation(environment, "parentEnvName");
        assertEquals("1. Parent environment should be in 'AVAILABLE' status.",
                validationResult.getFormattedErrors());
    }

    @Test
    void shouldFailOnParentEnvironmentHasParentEnvironmentAsWell() {
        Environment environment = aValidEnvirontmentWithParent();
        environment.getParentEnvironment().setParentEnvironment(new Environment());

        ValidationResult validationResult = underTest.validateParentChildRelation(environment, "parentEnvName");
        assertEquals("1. Parent environment is already a child environment.",
                validationResult.getFormattedErrors());
    }

    @Test
    void shouldFailOnParentEnvironmentCloudPlatformNotSupported() {
        Environment environment = aValidEnvirontmentWithParent();
        environment.getParentEnvironment().setCloudPlatform(CloudPlatform.GCP.name());

        ValidationResult validationResult = underTest.validateParentChildRelation(environment, "parentEnvName");
        assertEquals("1. 'GCP' platform is not supported for parent environment.", validationResult.getFormattedErrors());
    }

    @Test
    void shouldFailOnChildEnvironmentCloudPlatformNotSupported() {
        Environment environment = aValidEnvirontmentWithParent();
        environment.setCloudPlatform(CloudPlatform.GCP.name());

        ValidationResult validationResult = underTest.validateParentChildRelation(environment, "parentEnvName");
        assertEquals("1. 'GCP' platform is not supported for child environment.", validationResult.getFormattedErrors());
    }

    private Environment aValidEnvirontmentWithParent() {
        Environment parentEnvironment = new Environment();
        parentEnvironment.setCloudPlatform(CloudPlatform.AWS.name());
        parentEnvironment.setStatus(EnvironmentStatus.AVAILABLE);

        Environment environment = new Environment();
        environment.setAccountId(ACCOUNT);
        environment.setCloudPlatform(CloudPlatform.YARN.name());
        environment.setParentEnvironment(parentEnvironment);

        return environment;
    }

    @ParameterizedTest
    @MethodSource("freeIpaCreationArguments")
    void shouldValidateFreeIpaCreation(int instanceCountByGroup, int spotPercentage, boolean valid) {
        FreeIpaCreationDto freeIpaCreationDto = FreeIpaCreationDto.builder()
                .withInstanceCountByGroup(instanceCountByGroup)
                .withAws(FreeIpaCreationAwsParametersDto.builder()
                        .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                .withPercentage(spotPercentage)
                                .build())
                        .build())
                .build();

        ValidationResult validationResult = underTest.validateFreeIpaCreation(freeIpaCreationDto);
        assertEquals(!valid, validationResult.hasError());
    }

    private static Stream<Arguments> freeIpaCreationArguments() {
        return Stream.of(
                Arguments.of(1, 0, true),
                Arguments.of(1, 100, true),
                Arguments.of(1, 50, false),
                Arguments.of(2, 0, true),
                Arguments.of(2, 100, true),
                Arguments.of(2, 50, true)
        );
    }

}
