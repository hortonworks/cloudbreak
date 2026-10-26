package com.sequenceiq.freeipa.service.migration;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.util.TestConstants.ENV_CRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationValidationServiceTest {

    private static final String AWS_VARIANT = AwsConstants.AwsVariant.AWS_VARIANT.variant().value();

    private static final String AWS_NATIVE_VARIANT = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value();

    private static final String AWS_NATIVE_GOV_VARIANT = AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value();

    private static final String AZURE_VARIANT = AzureConstants.VARIANT.value();

    private static final String GCP_VARIANT = GcpConstants.GCP_VARIANT.value();

    private static final String OPENSTACK_VARIANT = OpenStackConstants.OPENSTACK_VARIANT.value();

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @InjectMocks
    private MultiAzMigrationValidationService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(entitlementService.isFreeIpaMultiAzMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        lenient().when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        DetailedEnvironmentResponse defaultEnvironment = mockEnvironment(AwsConstants.AWS_PLATFORM.value(), Set.of("us-east-1a", "us-east-1b"));
        lenient().when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenReturn(defaultEnvironment);
    }

    static Object[][] multiAzMigrationEntitlementScenarios() {
        return new Object[][]{
                // testCaseName, entitled, errorFragment
                {"not entitled", false, Optional.of("not entitled to use FreeIPA multi-AZ migration")},
                {"entitled", true, Optional.empty()},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("multiAzMigrationEntitlementScenarios")
    void testValidateMultiAzMigrationEntitlement(String name, boolean entitled, Optional<String> errorFragment) {
        when(entitlementService.isFreeIpaMultiAzMigrationEnabled(ACCOUNT_ID)).thenReturn(entitled);
        Stack stack = createStack(AWS_NATIVE_VARIANT, false, true, Set.of(availableInstance("i-001")), null);

        ValidationResult result = underTest.validateMultiAzMigrationRequest(ENV_CRN, ACCOUNT_ID, stack);

        assertResult(result, errorFragment);
    }

    static Object[][] awsVariantMigrationEntitlementScenarios() {
        return new Object[][]{
                // testCaseName, variant, awsVariantMigrationEntitled, errorFragment
                {"AWS_NATIVE variant - entitlement not required", AWS_NATIVE_VARIANT, false, Optional.empty()},
                {"AWS_NATIVE_GOV variant - entitlement not required", AWS_NATIVE_GOV_VARIANT, false, Optional.empty()},
                {"AWS variant - not entitled", AWS_VARIANT, false, Optional.of("not entitled to use AWS variant migration")},
                {"AWS variant - entitled", AWS_VARIANT, true, Optional.empty()},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("awsVariantMigrationEntitlementScenarios")
    void testValidateAwsVariantMigrationEntitlement(String name, String variant, boolean awsVariantMigrationEntitled, Optional<String> errorFragment) {
        lenient().when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(awsVariantMigrationEntitled);
        Stack stack = createStack(variant, false, true, Set.of(availableInstance("i-001")), imageWithOsType(OsType.RHEL8.getOsType()));

        ValidationResult result = underTest.validateMultiAzMigrationRequest(ENV_CRN, ACCOUNT_ID, stack);

        assertResult(result, errorFragment);
    }

    static Object[][] awsVariantOsSupportScenarios() {
        return new Object[][]{
                // testCaseName, variant, image errorFragment
                {"AWS_NATIVE variant - OS check skipped", AWS_NATIVE_VARIANT, null, Optional.empty()},
                {"AWS variant - null image", AWS_VARIANT, null, Optional.of("Could not retrieve the current OS")},
                {"AWS variant - null OS type in image", AWS_VARIANT, imageWithOsType(null), Optional.of("Could not retrieve the current OS")},
                {"AWS variant - CentOS7 (non-RHEL)", AWS_VARIANT, imageWithOsType(OsType.CENTOS7.getOsType()),
                        Optional.of("current OS does not support the AWS variant migration")},
                {"AWS variant - RHEL8", AWS_VARIANT, imageWithOsType(OsType.RHEL8.getOsType()), Optional.empty()},
                {"AWS variant - RHEL9", AWS_VARIANT, imageWithOsType(OsType.RHEL9.getOsType()), Optional.empty()},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("awsVariantOsSupportScenarios")
    void testValidateAwsVariantOsSupport(String name, String variant, ImageEntity image, Optional<String> errorFragment) {
        Stack stack = createStack(variant, false, true, Set.of(availableInstance("i-001")), image);

        ValidationResult result = underTest.validateMultiAzMigrationRequest(ENV_CRN, ACCOUNT_ID, stack);

        assertResult(result, errorFragment);
    }

    static Object[][] stackMultiAzFlagScenarios() {
        return new Object[][]{
                // testCaseName, alreadyMultiAz, detailedStatus, errorFragment
                {"stack already multi-AZ with AVAILABLE status", true, DetailedStackStatus.AVAILABLE, Optional.of("already multi-AZ enabled")},
                {"stack already multi-AZ with UPDATE_COMPLETE status", true, DetailedStackStatus.UPDATE_COMPLETE, Optional.of("already multi-AZ enabled")},
                {"stack already multi-AZ but previous migration failed - retry allowed", true, DetailedStackStatus.MULTI_AZ_MIGRATION_FAILED, Optional.empty()},
                {"stack already multi-AZ with null detailed status", true, null, Optional.of("already multi-AZ enabled")},
                {"stack not yet multi-AZ", false, DetailedStackStatus.AVAILABLE, Optional.empty()},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stackMultiAzFlagScenarios")
    void testValidateStackMultiAzFlag(String name, boolean alreadyMultiAz, DetailedStackStatus detailedStatus, Optional<String> errorFragment) {
        Stack stack = createStack(AWS_NATIVE_VARIANT, alreadyMultiAz, true, Set.of(availableInstance("i-001")), null, detailedStatus);

        ValidationResult result = underTest.validateMultiAzMigrationRequest(ENV_CRN, ACCOUNT_ID, stack);

        assertResult(result, errorFragment);
    }

    static Object[][] variantSupportScenarios() {
        return new Object[][]{
                // testCaseName, variant, errorFragment
                {"AWS_VARIANT supported", AWS_VARIANT, Optional.empty()},
                {"AWS_NATIVE_VARIANT supported", AWS_NATIVE_VARIANT, Optional.empty()},
                {"AWS_NATIVE_GOV_VARIANT supported", AWS_NATIVE_GOV_VARIANT, Optional.empty()},
                {"AZURE_VARIANT supported", AZURE_VARIANT, Optional.empty()},
                {"GCP_VARIANT supported", GCP_VARIANT, Optional.empty()},
                {"unsupported variant", OPENSTACK_VARIANT, Optional.of("Multi-AZ migration is not supported for platform variant")},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variantSupportScenarios")
    void testValidateVariantSupported(String name, String variant, Optional<String> errorFragment) {
        if (AZURE_VARIANT.equals(variant)) {
            DetailedEnvironmentResponse azureEnvironment = mockEnvironment("AZURE", Set.of("1", "2", "3"));
            lenient().when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenReturn(azureEnvironment);
        } else if (GCP_VARIANT.equals(variant)) {
            DetailedEnvironmentResponse gcpEnvironment = mockEnvironment("GCP", Set.of("us-west2-a", "us-west2-b", "us-west2-c"));
            lenient().when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenReturn(gcpEnvironment);
        }
        Stack stack = createStack(variant, false, true, Set.of(availableInstance("i-001")), imageWithOsType(OsType.RHEL8.getOsType()));

        ValidationResult result = underTest.validateMultiAzMigrationRequest(ENV_CRN, ACCOUNT_ID, stack);

        assertResult(result, errorFragment);
    }

    static Object[][] freeIpaStatusScenarios() {
        return new Object[][]{
                // testCaseName, stackAvailable, errorFragment
                {"FreeIPA is available", true, Optional.empty()},
                {"FreeIPA is not available", false, Optional.of("not in available state")},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("freeIpaStatusScenarios")
    void testValidateFreeIpaStatus(String name, boolean stackAvailable, Optional<String> errorFragment) {
        Stack stack = createStack(AWS_NATIVE_VARIANT, false, stackAvailable, Set.of(availableInstance("i-001")), null);

        ValidationResult result = underTest.validateMultiAzMigrationRequest(ENV_CRN, ACCOUNT_ID, stack);

        assertResult(result, errorFragment);
    }

    static Object[][] instanceStatusScenarios() {
        return new Object[][]{
                // testCaseName, instances, errorFragment
                {"all instances available", Set.of(availableInstance("i-001"), availableInstance("i-002")), Optional.empty()},
                {"one instance not available", Set.of(availableInstance("i-001"), notAvailableInstance("i-002")),
                        Optional.of("has non-available instances")},
                {"all instances not available", Set.of(notAvailableInstance("i-001"), notAvailableInstance("i-002")),
                        Optional.of("has non-available instances")},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("instanceStatusScenarios")
    void testValidateInstanceStatuses(String name, Set<InstanceMetaData> instances, Optional<String> errorFragment) {
        Stack stack = createStack(AWS_NATIVE_VARIANT, false, true, instances, null);

        ValidationResult result = underTest.validateMultiAzMigrationRequest(ENV_CRN, ACCOUNT_ID, stack);

        assertResult(result, errorFragment);
    }

    static Object[][] environmentZoneScenarios() {
        return new Object[][]{
                // testCaseName, cloudPlatform, azCount, nullNetwork, throwException, errorFragment
                {"environment service throws WebApplicationException", null, 0, false, true, Optional.of("Could not retrieve environment for validation")},
                {"null network on environment", "AWS", 0, true, false, Optional.of("Could not determine availability zones")},
                {"AWS environment with 0 AZs", "AWS", 0, false, false, Optional.of("less than 2 distinct availability zones")},
                {"AWS environment with 1 AZ", "AWS", 1, false, false, Optional.of("less than 2 distinct availability zones")},
                {"AWS environment with 2 AZs", "AWS", 2, false, false, Optional.empty()},
                {"AWS environment with 3 AZs", "AWS", 3, false, false, Optional.empty()},
                {"Azure environment with 0 AZs", "AZURE", 0, false, false, Optional.of("less than 2 distinct availability zones")},
                {"Azure environment with 1 AZ", "AZURE", 1, false, false, Optional.of("less than 2 distinct availability zones")},
                {"Azure environment with 2 AZs", "AZURE", 2, false, false, Optional.empty()},
                {"Azure environment with 3 AZs", "AZURE", 3, false, false, Optional.empty()},
                {"GCP environment with 0 AZs", "GCP", 0, false, false, Optional.of("less than 2 distinct availability zones")},
                {"GCP environment with 1 AZ", "GCP", 1, false, false, Optional.of("less than 2 distinct availability zones")},
                {"GCP environment with 2 AZs", "GCP", 2, false, false, Optional.empty()},
                {"GCP environment with 3 AZs", "GCP", 3, false, false, Optional.empty()},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("environmentZoneScenarios")
    void testValidateEnvironmentZones(String name, String cloudPlatform, int azCount, boolean nullNetwork, boolean throwException,
            Optional<String> errorFragment) {
        if (throwException) {
            when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenThrow(new WebApplicationException("Connection refused"));
        } else {
            DetailedEnvironmentResponse environment = nullNetwork
                    ? mockEnvironmentNullNetwork(cloudPlatform)
                    : mockEnvironment(cloudPlatform, generateAzs(azCount));
            when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenReturn(environment);
        }
        Stack stack = createStack(AWS_NATIVE_VARIANT, false, true, Set.of(availableInstance("i-001")), null);

        ValidationResult result = underTest.validateMultiAzMigrationRequest(ENV_CRN, ACCOUNT_ID, stack);

        assertResult(result, errorFragment);
    }

    private Stack createStack(String variant, boolean multiAz, boolean available, Set<InstanceMetaData> instances, ImageEntity image) {
        return createStack(variant, multiAz, available, instances, image, null);
    }

    private Stack createStack(String variant, boolean multiAz, boolean available, Set<InstanceMetaData> instances, ImageEntity image,
            DetailedStackStatus detailedStatus) {
        Stack stack = mock(Stack.class);
        when(stack.getPlatformvariant()).thenReturn(variant);
        when(stack.isMultiAz()).thenReturn(multiAz);
        when(stack.isAvailable()).thenReturn(available);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instances);
        lenient().when(stack.getImage()).thenReturn(image);
        if (detailedStatus != null) {
            StackStatus stackStatus = mock(StackStatus.class);
            lenient().when(stackStatus.getDetailedStackStatus()).thenReturn(detailedStatus);
            lenient().when(stack.getStackStatus()).thenReturn(stackStatus);
        }
        return stack;
    }

    private static InstanceMetaData availableInstance(String instanceId) {
        InstanceMetaData instance = new InstanceMetaData();
        instance.setInstanceId(instanceId);
        instance.setInstanceStatus(InstanceStatus.CREATED);
        return instance;
    }

    private static InstanceMetaData notAvailableInstance(String instanceId) {
        InstanceMetaData instance = new InstanceMetaData();
        instance.setInstanceId(instanceId);
        instance.setInstanceStatus(InstanceStatus.FAILED);
        return instance;
    }

    private static ImageEntity imageWithOsType(String osType) {
        ImageEntity image = new ImageEntity();
        image.setOsType(osType);
        return image;
    }

    private static DetailedEnvironmentResponse mockEnvironment(String cloudPlatform, Set<String> availabilityZones) {
        DetailedEnvironmentResponse environment = mock(DetailedEnvironmentResponse.class);
        EnvironmentNetworkResponse network = mock(EnvironmentNetworkResponse.class);
        lenient().when(environment.getCloudPlatform()).thenReturn(cloudPlatform);
        lenient().when(environment.getNetwork()).thenReturn(network);
        lenient().when(network.getAvailabilityZones(any())).thenReturn(availabilityZones);
        return environment;
    }

    private static DetailedEnvironmentResponse mockEnvironmentNullNetwork(String cloudPlatform) {
        DetailedEnvironmentResponse environment = mock(DetailedEnvironmentResponse.class);
        lenient().when(environment.getCloudPlatform()).thenReturn(cloudPlatform);
        lenient().when(environment.getNetwork()).thenReturn(null);
        return environment;
    }

    private static Set<String> generateAzs(int count) {
        return switch (count) {
            case 0 -> Set.of();
            case 1 -> Set.of("us-east-1a");
            case 2 -> Set.of("us-east-1a", "us-east-1b");
            case 3 -> Set.of("us-east-1a", "us-east-1b", "us-east-1c");
            default -> throw new IllegalArgumentException("Unsupported AZ count in test: " + count);
        };
    }

    private static void assertResult(ValidationResult result, Optional<String> errorFragment) {
        assertThat(result.hasError()).isEqualTo(errorFragment.isPresent());
        errorFragment.ifPresent(fragment -> assertThat(result.getFormattedErrors()).contains(fragment));
    }
}
