package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.service.database.DbOverrideConfig;

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
}