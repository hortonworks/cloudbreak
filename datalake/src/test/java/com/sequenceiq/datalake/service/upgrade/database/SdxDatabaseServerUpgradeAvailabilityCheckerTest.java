package com.sequenceiq.datalake.service.upgrade.database;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

@ExtendWith(MockitoExtension.class)
public class SdxDatabaseServerUpgradeAvailabilityCheckerTest {

    @Mock
    private StackDatabaseServerResponse stackDatabaseServerResponse;

    @InjectMocks
    private SdxDatabaseServerUpgradeAvailabilityChecker underTest;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUpgradeNeededWhenDatabaseEngineVersionSmallerThanTargetVersion(boolean forced) {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        when(stackDatabaseServerResponse.getMajorVersion()).thenReturn(MajorVersion.VERSION_10);

        boolean upgradeNeeded = underTest.isUpgradeNeeded(stackDatabaseServerResponse, targetMajorVersion, forced);

        assertTrue(upgradeNeeded);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUpgradeNeededWhenDatabaseEngineVersionEqualsTargetVersion(boolean forced) {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        when(stackDatabaseServerResponse.getMajorVersion()).thenReturn(MajorVersion.VERSION_11);

        boolean upgradeNeeded = underTest.isUpgradeNeeded(stackDatabaseServerResponse, targetMajorVersion, forced);

        if (forced) {
            assertTrue(upgradeNeeded);
        } else {
            assertFalse(upgradeNeeded);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testIsUpgradeNeededWhenExternalDbAndVersionUnknown(boolean forced) {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        when(stackDatabaseServerResponse.getMajorVersion()).thenReturn(null);

        boolean upgradeNeeded = underTest.isUpgradeNeeded(stackDatabaseServerResponse, targetMajorVersion, forced);

        assertTrue(upgradeNeeded);
    }

}