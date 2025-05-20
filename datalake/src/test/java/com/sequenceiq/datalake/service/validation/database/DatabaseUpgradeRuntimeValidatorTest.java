package com.sequenceiq.datalake.service.validation.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.service.database.DbOverrideConfig;

@ExtendWith(MockitoExtension.class)
public class DatabaseUpgradeRuntimeValidatorTest {

    private static final String RUNTIME_VERSION_MINIMUM_ACCEPTED_VERSION = "1.2.3";

    private static final String ACCOUNT_ID = "accountId";

    private static final String RUNTIME_VERSION_TOO_LOW = "1.2.2";

    private static final String ENGINE_VERSION = "14";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private DatabaseUpgradeRuntimeValidator underTest;

    @Mock
    private DbOverrideConfig dbOverrideConfig;

    @BeforeEach
    void setup() {
        when(dbOverrideConfig.findMinRuntimeVersion(ENGINE_VERSION)).thenReturn(Optional.of(RUNTIME_VERSION_MINIMUM_ACCEPTED_VERSION));
    }

    @Test
    void testValidateRuntimeVersionForUpgradeWhenVersionAtLeastTheMinimum() {
        try (MockedStatic<ThreadBasedUserCrnProvider> utilities = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            utilities.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);

            boolean upgradeAllowed = underTest.isRuntimeVersionAllowedForUpgrade(RUNTIME_VERSION_MINIMUM_ACCEPTED_VERSION, ENGINE_VERSION);

            assertTrue(upgradeAllowed);
        }
    }

    @Test
    void testValidateRuntimeVersionForUpgradeWhenVersionBelowMinimumButEntitlementPresent() {
        try (MockedStatic<ThreadBasedUserCrnProvider> utilities = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            utilities.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            when(entitlementService.isPostgresUpgradeExceptionEnabled(ACCOUNT_ID)).thenReturn(true);

            boolean upgradeAllowed = underTest.isRuntimeVersionAllowedForUpgrade(RUNTIME_VERSION_TOO_LOW, ENGINE_VERSION);

            assertTrue(upgradeAllowed);
        }
    }

    @Test
    void testValidateRuntimeVersionForUpgradeWhenVersionBelowMinimumAndNoEntitlement() {
        try (MockedStatic<ThreadBasedUserCrnProvider> utilities = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            utilities.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            when(entitlementService.isPostgresUpgradeExceptionEnabled(ACCOUNT_ID)).thenReturn(false);

            boolean upgradeAllowed = underTest.isRuntimeVersionAllowedForUpgrade(RUNTIME_VERSION_TOO_LOW, ENGINE_VERSION);

            assertFalse(upgradeAllowed);
        }
    }

    @Test
    void testGetMinRuntimeVersion() {
        assertEquals(RUNTIME_VERSION_MINIMUM_ACCEPTED_VERSION, underTest.getMinRuntimeVersion(ENGINE_VERSION).get());
    }

}
