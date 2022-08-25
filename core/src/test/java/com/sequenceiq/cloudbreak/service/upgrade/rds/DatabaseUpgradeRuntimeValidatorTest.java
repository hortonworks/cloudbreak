package com.sequenceiq.cloudbreak.service.upgrade.rds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
class DatabaseUpgradeRuntimeValidatorTest {

    private static final String ACCOUNT_ID = "accountId";

    @InjectMocks
    private DatabaseUpgradeRuntimeValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    static Object[][] testInput() {
        return new Object[][] {
                { "Test should not return error when entitlement enabled", "7.2.10", "7.2.7", true, false },
                { "Test should return error when runtime version is lower than the minimum and the entitlement disabled", "7.2.2", "7.2.7", false, true },
                { "Test should not return error when runtime version is the same as the minimum and the entitlement disabled", "7.2.7", "7.2.7", false, false },
                { "Test should return error when runtime version is higher than the minimum and the entitlement disabled", "7.2.10", "7.2.7", false, false },
                { "Test should return error when runtime version is null and the entitlement disabled", null, "7.2.7", false, true },
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testInput")
    void testCalculateDbVersionBasedOnRuntimeIfMissing(String name, String runtime, String minRuntime, boolean entitlementEnabled,
            boolean errorPresent) {
        ReflectionTestUtils.setField(underTest, "minRuntimeVersion", minRuntime);
        when(entitlementService.isPostgresUpgradeExceptionEnabled(ACCOUNT_ID)).thenReturn(entitlementEnabled);

        Optional<String> actual = underTest.validateRuntimeVersionForUpgrade(runtime, ACCOUNT_ID);

        assertEquals(errorPresent, actual.isPresent());
    }
}