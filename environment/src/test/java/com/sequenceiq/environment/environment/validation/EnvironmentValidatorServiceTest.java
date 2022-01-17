package com.sequenceiq.environment.environment.validation;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyArnValidator;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyUrlValidator;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyValidator;
import com.sequenceiq.environment.environment.validation.validators.NetworkCreationValidator;
import com.sequenceiq.environment.environment.validation.validators.PublicKeyValidator;
import com.sequenceiq.environment.environment.validation.validators.TagValidator;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;

@ExtendWith(MockitoExtension.class)
class EnvironmentValidatorServiceTest {

    private static final String ACCOUNT = "account";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:123:user:123";

    private static final String ENCRYPTION_KEY = "dummy-encryption-key";

    @Mock
    private NetworkCreationValidator networkCreationValidator;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private EnvironmentResourceService environmentResourceService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private PublicKeyValidator publicKeyValidator;

    @Mock
    private TagValidator tagValidator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EncryptionKeyArnValidator encryptionKeyArnValidator;

    @Mock
    private EncryptionKeyUrlValidator encryptionKeyUrlValidator;

    @Mock
    private EncryptionKeyValidator encryptionKeyValidator;

    private EnvironmentValidatorService underTest;

    @BeforeEach
    public void setUp() {
        underTest = new EnvironmentValidatorService(
                networkCreationValidator,
                platformParameterService,
                environmentResourceService,
                credentialService,
                publicKeyValidator,
                singleton(CloudPlatform.AWS.name()),
                singleton(CloudPlatform.YARN.name()),
                tagValidator,
                encryptionKeyArnValidator,
                encryptionKeyUrlValidator,
                entitlementService,
                encryptionKeyValidator);
    }

    @Test
    void testValidateAwsEnvironmentRequestNotAWS() {
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn("AZURE");

        EnvironmentRequest request = new EnvironmentRequest();
        request.setCredentialName("azure-credential");
        ValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateAwsEnvironmentRequest(request));
        assertTrue(result.hasError());
        assertEquals("Environment request is not for cloud platform AWS.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestNoAwsParams() {
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn("AWS");

        EnvironmentRequest request = new EnvironmentRequest();
        request.setCredentialName("aws-credential");
        ValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateAwsEnvironmentRequest(request));
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentNoS3GuardParams() {
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn("AWS");

        EnvironmentRequest request = new EnvironmentRequest();
        request.setCredentialName("aws-credential");
        request.setAws(new AwsEnvironmentParameters());
        ValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateAwsEnvironmentRequest(request));
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestNoDynamoTable() {
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn("AWS");

        EnvironmentRequest request = new EnvironmentRequest();
        request.setCredentialName("aws-credential");
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        aws.setS3guard(new S3GuardRequestParameters());
        request.setAws(aws);
        ValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateAwsEnvironmentRequest(request));
        assertTrue(result.hasError());
        assertEquals("S3Guard Dynamo DB table name is not found in environment request.", result.getErrors().get(0));
    }

    @Test
    void testValidateAwsEnvironmentRequestValid() {
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn("AWS");

        EnvironmentRequest request = new EnvironmentRequest();
        request.setCredentialName("aws-credential");
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        S3GuardRequestParameters s3GuardRequestParameters = new S3GuardRequestParameters();
        s3GuardRequestParameters.setDynamoDbTableName("table");
        aws.setS3guard(s3GuardRequestParameters);
        request.setAws(aws);
        ValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateAwsEnvironmentRequest(request));
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
        assertEquals("The CIDR can be replaced with the default and knox security groups, please add to the request", validationResult.getFormattedErrors());
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
        assertEquals("The CIDR could not be updated in the environment", validationResult.getFormattedErrors());
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
        assertEquals("The change of publicKeyId is not supported on AZURE", validationResult.getFormattedErrors());
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
        when(publicKeyValidator.validatePublicKey(anyString())).thenReturn(ValidationResult.empty());

        ValidationResult validationResult = underTest.validateAuthenticationModification(environmentEditDto, environment);
        assertEquals("You should define either publicKey or publicKeyId only, but not both.", validationResult.getFormattedErrors());
    }

    @Test
    void testValidateAuthenticationModificationWhenPublicKeyAndPublicKeyIdIsEmptyAsWell() {
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder()
                .withAuthentication(AuthenticationDto.builder().build())
                .build();

        ValidationResult validationResult = underTest.validateAuthenticationModification(environmentEditDto, environment);
        assertEquals("You should define either the publicKey or the publicKeyId.", validationResult.getFormattedErrors());
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
        assertEquals("The publicKeyId with name of 'pub-key-id' does not exist on the provider.", validationResult.getFormattedErrors());
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
        assertEquals("Active parent environment with name 'parentEnvName' is not available in account '" + ACCOUNT + "'.",
                validationResult.getFormattedErrors());
    }

    @Test
    void shouldFailOnParentEnvironmentInNonActiveState() {
        Environment environment = aValidEnvirontmentWithParent();
        environment.getParentEnvironment().setStatus(EnvironmentStatus.ARCHIVED);
        ValidationResult validationResult = underTest.validateParentChildRelation(environment, "parentEnvName");
        assertEquals("Parent environment should be in 'AVAILABLE' status.",
                validationResult.getFormattedErrors());
    }

    @Test
    void shouldFailOnParentEnvironmentHasParentEnvironmentAsWell() {
        Environment environment = aValidEnvirontmentWithParent();
        environment.getParentEnvironment().setParentEnvironment(new Environment());

        ValidationResult validationResult = underTest.validateParentChildRelation(environment, "parentEnvName");
        assertEquals("Parent environment is already a child environment.",
                validationResult.getFormattedErrors());
    }

    @Test
    void shouldFailOnParentEnvironmentCloudPlatformNotSupported() {
        Environment environment = aValidEnvirontmentWithParent();
        environment.getParentEnvironment().setCloudPlatform(CloudPlatform.GCP.name());

        ValidationResult validationResult = underTest.validateParentChildRelation(environment, "parentEnvName");
        assertEquals("'GCP' platform is not supported for parent environment.", validationResult.getFormattedErrors());
    }

    @Test
    void shouldFailOnChildEnvironmentCloudPlatformNotSupported() {
        Environment environment = aValidEnvirontmentWithParent();
        environment.setCloudPlatform(CloudPlatform.GCP.name());

        ValidationResult validationResult = underTest.validateParentChildRelation(environment, "parentEnvName");
        assertEquals("'GCP' platform is not supported for child environment.", validationResult.getFormattedErrors());
    }

    @Test
    void shouldFailIfEncryptionKeyUrlSpecifiedAndEntitlementAndWrongFormat() {
        String encryptionKeyUrl = "Dummy-key-url";
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        validationResultBuilder.error("error");
        when(encryptionKeyUrlValidator.validateEncryptionKeyUrl(any())).thenReturn(validationResultBuilder.build());
        when(entitlementService.isAzureDiskSSEWithCMKEnabled(any())).thenReturn(true);
        ValidationResult validationResult = underTest.validateEncryptionKeyUrl(encryptionKeyUrl, ACCOUNT_ID);
        assertTrue(validationResult.hasError());
    }

    @Test
    void shouldFailIfEncryptionKeyArnSpecifiedAndEntitlementDisabled() {
        String encryptionKeyArn = "dummy-key-arn";
        when(entitlementService.isAWSDiskEncryptionWithCMKEnabled(any())).thenReturn(false);
        ValidationResult validationResult = underTest.validateEncryptionKeyArn(encryptionKeyArn, ACCOUNT_ID);
        assertTrue(validationResult.hasError());
    }

    @Test
    void testValidateEncryptionKeyArnSpecifiedAndEntitlementEnabled() {
        String encryptionKeyArn = "dummy-key-arn";
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        when(encryptionKeyArnValidator.validateEncryptionKeyArn(any())).thenReturn(validationResultBuilder.build());
        when(entitlementService.isAWSDiskEncryptionWithCMKEnabled(any())).thenReturn(true);
        ValidationResult validationResult = underTest.validateEncryptionKeyArn(encryptionKeyArn, ACCOUNT_ID);
        assertFalse(validationResult.hasError());
    }

    @Test
    void shouldFailIfEncryptionKeyUrlSpecifiedAndNotEntitlement() {
        String encryptionKeyUrl = "Dummy-key-url";
        when(entitlementService.isAzureDiskSSEWithCMKEnabled(any())).thenReturn(false);
        ValidationResult validationResult = underTest.validateEncryptionKeyUrl(encryptionKeyUrl, ACCOUNT_ID);
        assertTrue(validationResult.hasError());
    }

    @Test
    void testValidateEncryptionKeyUrlSpecifiedAndEntitlement() {
        String encryptionKeyUrl = "https://someVault.vault.azure.net/keys/someKey/someKeyVersion";
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        when(encryptionKeyUrlValidator.validateEncryptionKeyUrl(any())).thenReturn(validationResultBuilder.build());
        when(entitlementService.isAzureDiskSSEWithCMKEnabled(any())).thenReturn(true);
        ValidationResult validationResult = underTest.validateEncryptionKeyUrl(encryptionKeyUrl, ACCOUNT_ID);
        assertFalse(validationResult.hasError());
    }

    @Test
    void shouldFailIfGcpEncryptionKeySpecifiedAndEntitlementAndWrongFormat() {
        EnvironmentCreationDto creationDto = EnvironmentCreationDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withCloudPlatform("GCP")
                .withParameters(ParametersDto.builder()
                        .withGcpParameters(GcpParametersDto.builder()
                                .withEncryptionParameters(GcpResourceEncryptionParametersDto.builder()
                                        .withEncryptionKey("project/Wrong-dummy-key-format")
                                        .build())
                                .build())
                        .build())
                .build();

        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        validationResultBuilder.error("error");
        when(encryptionKeyValidator.validateEncryptionKey(any())).thenReturn(validationResultBuilder.build());
        when(entitlementService.isGcpDiskEncryptionWithCMEKEnabled(any())).thenReturn(true);
        ValidationResult validationResult = underTest.validateEncryptionKey(creationDto);
        assertTrue(validationResult.hasError());
    }

    @Test
    void shouldFailIfGcpEncryptionKeySpecifiedAndNotEntitlement() {
        EnvironmentCreationDto creationDto = EnvironmentCreationDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withCloudPlatform("GCP")
                .withParameters(ParametersDto.builder()
                        .withGcpParameters(GcpParametersDto.builder()
                                .withEncryptionParameters(GcpResourceEncryptionParametersDto.builder()
                                        .withEncryptionKey("project/Wrong-dummy-key-format")
                                        .build())
                                .build())
                        .build())
                .build();

        when(entitlementService.isGcpDiskEncryptionWithCMEKEnabled(any())).thenReturn(false);
        ValidationResult validationResult = underTest.validateEncryptionKey(creationDto);

        assertTrue(validationResult.hasError());
    }

    @Test
    void testValidateGcpEncryptionKeyNotSpecified() {
        EnvironmentCreationDto creationDto = EnvironmentCreationDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withCloudPlatform("GCP")
                .build();
        ValidationResult validationResult = underTest.validateEncryptionKey(creationDto);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateGcpEncryptionKeySpecifiedAndEntitlement() {
        EnvironmentCreationDto creationDto = EnvironmentCreationDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withCloudPlatform("GCP")
                .withParameters(ParametersDto.builder()
                        .withGcpParameters(GcpParametersDto.builder()
                                .withEncryptionParameters(GcpResourceEncryptionParametersDto.builder()
                                        .withEncryptionKey("projects/dummy-project/locations/us-west2/keyRings/dummy-ring/cryptoKeys/dummy-key")
                                        .build())
                                .build())
                        .build())
                .build();

        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();

        when(encryptionKeyValidator.validateEncryptionKey(any())).thenReturn(validationResultBuilder.build());
        when(entitlementService.isGcpDiskEncryptionWithCMEKEnabled(any())).thenReturn(true);

        ValidationResult validationResult = underTest.validateEncryptionKey(creationDto);
        assertFalse(validationResult.hasError());
    }

    @ParameterizedTest
    @MethodSource("storageValidationArguments")
    void testStorageLocation(String storageLocation, boolean valid) {
        ValidationResult actual = underTest.validateStorageLocation(storageLocation, "any");
        assertEquals(!valid, actual.hasError());
    }

    @Test
    void testStorageLocationWhenNull() {
        ValidationResult validationResult = underTest.validateStorageLocation(null, "any");
        assertEquals(1, validationResult.getErrors().size());
        assertEquals("You don't add a(n) any storage location, please provide a valid storage location.", validationResult.getErrors().get(0));
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

    private static Stream<Arguments> storageValidationArguments() {
        return Stream.of(
                Arguments.of("location with whitespace", false),
                Arguments.of("   /path", true),
                Arguments.of("/path    ", true),
                Arguments.of("    /path/path   ", true),
                Arguments.of("/path  /  path", false),
                Arguments.of("/pa th/", false),
                Arguments.of("\t/path/path", true),
                Arguments.of("/path/path\t", true),
                Arguments.of("/path/\tpath", false),
                Arguments.of("\n/path/path", true),
                Arguments.of("/path/path\n", true),
                Arguments.of("/path/\npath", false)
        );
    }

}
