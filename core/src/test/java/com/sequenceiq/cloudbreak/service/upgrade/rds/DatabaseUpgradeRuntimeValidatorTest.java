package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.service.database.DbOverrideConfig;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;

@ExtendWith(MockitoExtension.class)
class DatabaseUpgradeRuntimeValidatorTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENGINE_VERSION = "14";

    private static final String RUNTIME_VERSION_MINIMUM_ACCEPTED_VERSION = "7.2.7";

    @InjectMocks
    private DatabaseUpgradeRuntimeValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DbOverrideConfig dbOverrideConfig;

    @Mock
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

static Object[][] testInput() {
        return new Object[][] {
                { "Test should not return error when entitlement enabled", "7.2.10", "14", true, false },
                { "Test should return error when runtime version is lower than the minimum and the entitlement disabled", "7.2.2", "14", false, true },
                { "Test should not return error when runtime version is the same as the minimum and the entitlement disabled", "7.2.7", "14", false, false },
                { "Test  when runtime version is the same as the minimum, target version is smaller then minimum engine version and the entitlement disabled",
                        "7.2.7", "11", false, true },
                { "Test should return error when runtime version is higher than the minimum and the entitlement disabled", "7.2.10", "14", false, false },
                { "Test should return error when runtime version is null and the entitlement disabled", null, "14", false, true },
        };
    }

    @BeforeEach
    void setup() {
        lenient().when(dbOverrideConfig.findMinRuntimeVersion(ENGINE_VERSION)).thenReturn(Optional.of(RUNTIME_VERSION_MINIMUM_ACCEPTED_VERSION));
        lenient().when(dbOverrideConfig.findMinRuntimeVersion("17")).thenReturn(Optional.of("7.3.2"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testInput")
    void testCalculateDbVersionBasedOnRuntimeIfMissing(String name, String runtime, String targetEngineVersion, boolean entitlementEnabled,
            boolean errorPresent) {
        when(entitlementService.isPostgresUpgradeExceptionEnabled(ACCOUNT_ID)).thenReturn(entitlementEnabled);

        Optional<String> actual = underTest.validateRuntimeVersionForUpgrade(runtime, targetEngineVersion, ACCOUNT_ID);

        assertEquals(errorPresent, actual.isPresent());
    }

    @Test
    void testValidateTargetMajorVersionAvailability() {
        // GIVEN
        StackView stackView = mock(StackView.class);
        Architecture architecture = mock(Architecture.class);

        String environmentCrn = "env-crn";
        String region = "us-west-1";
        String platformVariant = "aws";
        String availabilityZone = "us-west-1a";
        String architectureName = "x86_64";
        String cloudPlatform = "AWS";

        String currentEngineVersion = "13";
        String targetMajorVersion = "14";

        when(stackView.getEnvironmentCrn()).thenReturn(environmentCrn);
        when(stackView.getRegion()).thenReturn(region);
        when(stackView.getPlatformVariant()).thenReturn(platformVariant);
        when(stackView.getAvailabilityZone()).thenReturn(availabilityZone);
        when(stackView.getArchitecture()).thenReturn(architecture);
        when(architecture.getName()).thenReturn(architectureName);
        when(stackView.getCloudPlatform()).thenReturn(cloudPlatform);

        PlatformDatabaseCapabilitiesResponse databaseCapabilities = mock(PlatformDatabaseCapabilitiesResponse.class);
        Map<String, Map<String, List<String>>> regionUpgradeVersions = new HashMap<>();
        Map<String, List<String>> upgradeVersions = new HashMap<>();
        upgradeVersions.put(currentEngineVersion, List.of("14", "15"));
        regionUpgradeVersions.put(region, upgradeVersions);

        when(databaseCapabilities.getRegionUpgradeVersions()).thenReturn(regionUpgradeVersions);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                environmentCrn, region, platformVariant, availabilityZone,
                DatabaseCapabilityType.DEFAULT, architectureName))
                .thenReturn(databaseCapabilities);

        // WHEN
        Optional<String> result = underTest.validateTargetMajorVersionAvailability(targetMajorVersion, currentEngineVersion, stackView);

        // THEN
        assertEquals(Optional.empty(), result);

        // WHEN - Testing unavailable target version
        String unavailableTargetVersion = "16";
        result = underTest.validateTargetMajorVersionAvailability(unavailableTargetVersion, currentEngineVersion, stackView);

        // THEN
        assertTrue(result.isPresent());
        assertEquals(String.format("The DB target major version %s is not supported in region %s for platform %s. Supported versions are: %s",
                unavailableTargetVersion, region, platformVariant, upgradeVersions.get(currentEngineVersion)), result.get());

        // WHEN - Testing empty region upgrade versions
        when(databaseCapabilities.getRegionUpgradeVersions()).thenReturn(new HashMap<>());
        result = underTest.validateTargetMajorVersionAvailability(targetMajorVersion, currentEngineVersion, stackView);

        // THEN
        assertEquals(Optional.empty(), result);

        // WHEN - Testing missing current engine version
        regionUpgradeVersions = new HashMap<>();
        regionUpgradeVersions.put(region, new HashMap<>());
        when(databaseCapabilities.getRegionUpgradeVersions()).thenReturn(regionUpgradeVersions);
        result = underTest.validateTargetMajorVersionAvailability(targetMajorVersion, currentEngineVersion, stackView);

        // THEN
        assertEquals(Optional.empty(), result);
    }
}