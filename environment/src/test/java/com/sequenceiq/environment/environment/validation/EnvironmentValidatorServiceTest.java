package com.sequenceiq.environment.environment.validation;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.recipe.EnvironmentRecipeService;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyArnValidator;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyUrlValidator;
import com.sequenceiq.environment.environment.validation.validators.EncryptionKeyValidator;
import com.sequenceiq.environment.environment.validation.validators.ManagedIdentityRoleValidator;
import com.sequenceiq.environment.environment.validation.validators.NetworkValidator;
import com.sequenceiq.environment.environment.validation.validators.PublicKeyValidator;
import com.sequenceiq.environment.environment.validation.validators.TagValidator;
import com.sequenceiq.environment.platformresource.PlatformParameterService;

@ExtendWith(MockitoExtension.class)
class EnvironmentValidatorServiceTest {

    private static final String ACCOUNT = "account";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:123:user:123";

    private static final int FREE_IPA_INSTANCE_COUNT_BY_GROUP = 2;

    @Mock
    private NetworkValidator networkCreationValidator;

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

    @Mock
    private ManagedIdentityRoleValidator encryptionRoleValidator;

    @Mock
    private EnvironmentRecipeService recipeService;

    private EnvironmentValidatorService underTest;

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
                Arguments.of("   /path", true),
                Arguments.of("/path    ", true),
                Arguments.of("    /path/path   ", true),
                Arguments.of("\t/path/path", true),
                Arguments.of("/path/path\t", true),
                Arguments.of("\n/path/path", true),
                Arguments.of("/path/path\n", true),
                Arguments.of("wasb://asdf/asdf-v/apps/hive/warehouse", true),
                Arguments.of("s3a://asdf/asdf-v/apps/hive-something/", true),
                Arguments.of("https://mystorageaccount.blob.core.windows.net/data/", true),
                Arguments.of("gs://asdf/asdf-v/apps/hive-something/", true),
                Arguments.of("location with whitespace", false),
                Arguments.of("/path  /  path", false),
                Arguments.of("/pa th/", false),
                Arguments.of("/path/\tpath", false),
                Arguments.of("/path/\npath", false)

        );
    }

    @BeforeEach
    void setUp() {
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
                encryptionKeyValidator,
                recipeService,
                encryptionRoleValidator,
                1);
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
        assertFalse(result.hasError());
    }

    @Test
    void testValidateAwsEnvironmentNoS3GuardParams() {
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn("AWS");

        EnvironmentRequest request = new EnvironmentRequest();
        request.setCredentialName("aws-credential");
        request.setAws(new AwsEnvironmentParameters());
        ValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateAwsEnvironmentRequest(request));
        assertFalse(result.hasError());
    }

    @Test
    void testValidateAwsEnvironmentRequestNoDynamoTable() {
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn("AWS");

        EnvironmentRequest request = new EnvironmentRequest();
        request.setCredentialName("aws-credential");
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
        request.setAws(aws);
        ValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateAwsEnvironmentRequest(request));
        assertFalse(result.hasError());
    }

    @Test
    void testValidateAwsEnvironmentRequestValid() {
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn("AWS");

        EnvironmentRequest request = new EnvironmentRequest();
        request.setCredentialName("aws-credential");
        AwsEnvironmentParameters aws = new AwsEnvironmentParameters();
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
    void shouldFailIfEncryptionKeyUrlSpecifiedAndWrongFormat() {
        String encryptionKeyUrl = "Dummy-key-url";
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        validationResultBuilder.error("error");
        when(encryptionKeyUrlValidator.validateEncryptionKeyUrl(any())).thenReturn(validationResultBuilder.build());
        ValidationResult validationResult = underTest.validateEncryptionKeyUrl(encryptionKeyUrl);
        assertTrue(validationResult.hasError());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {true, false})
    void testValidateEncryptionKeyArnSpecified(boolean secretEncryptionEnabled) {
        String encryptionKeyArn = "dummy-key-arn";
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        when(encryptionKeyArnValidator.validateEncryptionKeyArn(eq("dummy-key-arn"), eq(secretEncryptionEnabled)))
                .thenReturn(validationResultBuilder.build());
        ValidationResult validationResult = underTest.validateEncryptionKeyArn(encryptionKeyArn, secretEncryptionEnabled);
        assertFalse(validationResult.hasError());
    }

    @Test
    void shouldFailIfGcpEncryptionKeySpecifiedAndWrongFormat() {
        String encryptionKey = "project/Wrong-dummy-key-format";
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        validationResultBuilder.error("error");
        when(encryptionKeyValidator.validateEncryptionKey(any())).thenReturn(validationResultBuilder.build());
        ValidationResult validationResult = underTest.validateEncryptionKey(encryptionKey);
        assertTrue(validationResult.hasError());
    }

    @ParameterizedTest
    @MethodSource("storageValidationArguments")
    void testStorageLocation(String storageLocation, boolean valid) {
        ValidationResult actual = underTest.validateStorageLocation(storageLocation, "any");
        assertEquals(!valid, actual.hasError(), "Storage location used: " + storageLocation);
    }

    @Test
    void testStorageLocationWhenNull() {
        ValidationResult validationResult = underTest.validateStorageLocation(null, "any");
        assertEquals(1, validationResult.getErrors().size());
        assertEquals("You don't add a(n) any storage location, please provide a valid storage location.", validationResult.getErrors().get(0));
    }

    @Test
    void testValidateWhenRequestedInstanceCountLessThanTheMinimumThreshold() {
        FreeIpaCreationDto freeIpaCreationDto = FreeIpaCreationDto.builder(0)
                .build();

        ValidationResult validationResult = underTest.validateFreeIpaCreation(freeIpaCreationDto, "accountId");
        assertTrue(validationResult.hasError());
        assertEquals("FreeIpa deployment requests are only allowed with at least '1' instance(s) by group. The requested value was '0'",
                validationResult.getErrors().get(0));
    }

    @Test
    void testValidateWhenRequestedInstanceCountEqualsOrMoreThanTheMinimumThresholdWithOutEnforcedSeLinuxNoEntitlementGranted() {
        when(entitlementService.isCdpSecurityEnforcingSELinux(any())).thenReturn(false);
        FreeIpaCreationDto freeIpaCreationDto = FreeIpaCreationDto.builder(1)
                .withSeLinux(SeLinux.ENFORCING)
                .build();

        ValidationResult validationResult = underTest.validateFreeIpaCreation(freeIpaCreationDto, "accountId");
        assertTrue(validationResult.hasError());
        assertEquals("SELinux enforcing requires CDP_SECURITY_ENFORCING_SELINUX entitlement for your account.",
                validationResult.getErrors().get(0));
    }

    @Test
    void testValidateWhenRequestedInstanceCountEqualsOrMoreThanTheMinimumThresholdWithEnforcedSeLinuxNoEntitlementGranted() {
        when(entitlementService.isCdpSecurityEnforcingSELinux(any())).thenReturn(true);
        FreeIpaCreationDto freeIpaCreationDto = FreeIpaCreationDto.builder(1)
                .withSeLinux(SeLinux.ENFORCING)
                .build();

        ValidationResult validationResult = underTest.validateFreeIpaCreation(freeIpaCreationDto, "accountId");
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateWhenRequestedFreeipaHasImageIdAndImageOs() {
        FreeIpaCreationDto freeIpaCreationDto = FreeIpaCreationDto.builder(1)
                .withImageId("id")
                .withImageOs("os")
                .build();

        ValidationResult validationResult = underTest.validateFreeIpaCreation(freeIpaCreationDto, "accountId");
        assertTrue(validationResult.hasError());
        assertEquals("FreeIpa deployment requests can not have both image id and image os parameters set.",
                validationResult.getErrors().get(0));
    }

    @Test
    void testValidateEncryptionRoleValidRole() {
        String encryptionKeyRole = "validRole";
        when(encryptionRoleValidator.validateEncryptionRole(any()))
                .thenReturn(ValidationResult.builder().build());

        ValidationResult result = underTest.validateEncryptionRole(encryptionKeyRole);

        assertFalse(result.hasError());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void testValidateEncryptionRoleInvalidRole() {
        String encryptionKeyRole = "invalidRole";
        when(encryptionRoleValidator.validateEncryptionRole(any()))
                .thenReturn(ValidationResult.builder().error("Invalid role").build());

        ValidationResult result = underTest.validateEncryptionRole(encryptionKeyRole);

        assertTrue(result.hasError());
        assertEquals(1, result.getErrors().size());
        assertEquals("Invalid role", result.getErrors().get(0));
    }

    @Test
    void testValidateExternalizedComputeClusterWhenPrivateClusterEnabledAndKubeApiAuthorizedIpRangesSpecified() {
        ValidationResult validationResult = underTest.validateExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                .withCreate(true).withPrivateCluster(true).withKubeApiAuthorizedIpRanges(Set.of("1.1.1.1/1")).build(), ACCOUNT, Set.of("subnet1", "subnet2"));
        assertTrue(validationResult.hasError());
        assertThat(validationResult.getErrors()).hasSize(1);
        assertThat(validationResult.getErrors())
                .containsOnly("The 'kubeApiAuthorizedIpRanges' parameter cannot be specified when 'privateCluster' is enabled.");
    }

    @Test
    void testValidateExternalizedComputeClusterWhenPrivateClusterDisabledAndKubeApiAuthorizedIpRangesNotSpecifiedAndNotInternal() {
        ValidationResult validationResult = underTest.validateExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                .withCreate(true).withPrivateCluster(false).build(), ACCOUNT, Set.of("subnet1", "subnet2"));
        assertTrue(validationResult.hasError());
        assertThat(validationResult.getErrors()).hasSize(1);
        assertThat(validationResult.getErrors())
                .containsOnly("The 'kubeApiAuthorizedIpRanges' parameter must be specified when 'privateCluster' is disabled.");
    }

    @Test
    void testValidateExternalizedComputeClusterWhenPrivateClusterDisabledAndKubeApiAuthorizedIpRangesNotSpecifiedAndInternal() {
        when(entitlementService.internalTenant(eq(ACCOUNT))).thenReturn(true);
        ValidationResult validationResult = underTest.validateExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                .withCreate(true).withPrivateCluster(false).build(), ACCOUNT, Set.of("subnet1", "subnet2"));
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateExternalizedComputeClusterWhenKubeApiAuthorizedIpRangesContainsDisallowedCidrAndNotInternalTenant() {
        ValidationResult validationResult = underTest.validateExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                .withCreate(true).withPrivateCluster(false).withKubeApiAuthorizedIpRanges(Set.of("0.0.0.0/0")).build(), ACCOUNT, Set.of("subnet1", "subnet2"));
        assertTrue(validationResult.hasError());
        assertThat(validationResult.getErrors()).hasSize(1);
        assertThat(validationResult.getErrors())
                .containsOnly("The value '0.0.0.0/0' is not allowed for 'kubeApiAuthorizedIpRanges'.");
    }

    @Test
    void testValidateExternalizedComputeClusterWhenKubeApiAuthorizedIpRangesContainsDisallowedCidrAndInternalTenant() {
        when(entitlementService.internalTenant(eq(ACCOUNT))).thenReturn(true);
        ValidationResult validationResult = underTest.validateExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                .withCreate(true).withPrivateCluster(false).withKubeApiAuthorizedIpRanges(Set.of("0.0.0.0/0")).build(), ACCOUNT, Set.of("subnet1", "subnet2"));
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateExternalizedComputeClusterWhenPrivateClusterDisabledAndKubeApiAuthorizedIpRangesSpecifiedValidCidr() {
        ValidationResult validationResult = underTest.validateExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                .withCreate(true).withPrivateCluster(false).withKubeApiAuthorizedIpRanges(Set.of("1.1.1.1/1")).build(), ACCOUNT, Set.of("subnet1", "subnet2"));
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateExternalizedComputeClusterWhenPrivateClusterEnabledAndKubeApiAuthorizedIpRangesNotSpecified() {
        ValidationResult validationResult = underTest.validateExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                .withCreate(true).withPrivateCluster(true).withWorkerNodeSubnetIds(Set.of("subnet1")).build(), ACCOUNT, Set.of("subnet1", "subnet2"));
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateExternalizedComputeClusterWhenBadSubnetsSpecified() {
        Set<String> environmentSubnets = Set.of("subnet1", "subnet2");
        ValidationResult validationResult = underTest.validateExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                .withCreate(true).withPrivateCluster(true).withWorkerNodeSubnetIds(Set.of("subnet1", "subnet3")).build(), ACCOUNT, environmentSubnets);
        assertTrue(validationResult.hasError());
        assertThat(validationResult.getErrors())
                .containsOnly("Specified compute cluster subnet 'subnet3' does not exist in the environment");
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
        FreeIpaCreationDto freeIpaCreationDto = FreeIpaCreationDto.builder(instanceCountByGroup)
                .withAws(FreeIpaCreationAwsParametersDto.builder()
                        .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                .withPercentage(spotPercentage)
                                .build())
                        .build())
                .build();

        ValidationResult validationResult = underTest.validateFreeIpaCreation(freeIpaCreationDto, "accountId");
        assertEquals(!valid, validationResult.hasError());
    }

}
