package com.sequenceiq.datalake.service.upgrade.database;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.datalake.entity.SdxCluster;

@ExtendWith(MockitoExtension.class)
public class SdxDatabaseServerUpgradeAvailabilityCheckerTest {

    @Mock
    private DatabaseEngineVersionReaderService databaseEngineVersionReaderService;

    @InjectMocks
    private SdxDatabaseServerUpgradeAvailabilityChecker underTest;

    @Test
    void testUpgradeNeededWhenDatabaseEngineVersionSmallerThanTargetVersion() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setDatabaseEngineVersion("10.6");

        boolean upgradeNeeded = underTest.isUpgradeNeeded(sdxCluster, targetMajorVersion);

        assertTrue(upgradeNeeded);
    }

    @Test
    void testUpgradeNeededWhenDatabaseEngineVersionEqualsTargetVersion() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setDatabaseEngineVersion("11.0");

        boolean upgradeNeeded = underTest.isUpgradeNeeded(sdxCluster, targetMajorVersion);

        assertFalse(upgradeNeeded);
    }

    @Test
    void testUpgradeNeededWhenExternalDbAndVersionSmallerThanTarget() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = new SdxCluster();

        boolean upgradeNeeded = underTest.isUpgradeNeeded(sdxCluster, targetMajorVersion);

        assertTrue(upgradeNeeded);
    }

    @Test
    void testUpgradeNeededWhenExternalDbAndVersionEqualsTarget() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = new SdxCluster();
        when(databaseEngineVersionReaderService.getDatabaseServerMajorVersion(sdxCluster)).thenReturn(Optional.of(MajorVersion.VERSION_11));

        boolean upgradeNeeded = underTest.isUpgradeNeeded(sdxCluster, targetMajorVersion);

        assertFalse(upgradeNeeded);
    }

    @Test
    void testUpgradeNeededWhenEmbeddedDbAndVersionSmallerThanTarget() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = new SdxCluster();
        when(databaseEngineVersionReaderService.getDatabaseServerMajorVersion(sdxCluster)).thenReturn(Optional.of(MajorVersion.VERSION_10));

        boolean upgradeNeeded = underTest.isUpgradeNeeded(sdxCluster, targetMajorVersion);

        assertTrue(upgradeNeeded);
    }

    @Test
    void testUpgradeNeededWhenEmbeddedDbAndVersionEqualsTarget() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = new SdxCluster();
        when(databaseEngineVersionReaderService.getDatabaseServerMajorVersion(sdxCluster)).thenReturn(Optional.of(MajorVersion.VERSION_11));

        boolean upgradeNeeded = underTest.isUpgradeNeeded(sdxCluster, targetMajorVersion);

        assertFalse(upgradeNeeded);
    }

    @Test
    void testIsUpgradeNeededWhenExternalDbAndVersionUnknown() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = new SdxCluster();
        when(databaseEngineVersionReaderService.getDatabaseServerMajorVersion(sdxCluster)).thenReturn(Optional.empty());

        boolean upgradeNeeded = underTest.isUpgradeNeeded(sdxCluster, targetMajorVersion);

        assertTrue(upgradeNeeded);
    }

    @Test
    void testIsUpgradeNeededWhenEmbeddedDbAndVersionUnknown() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = new SdxCluster();
        when(databaseEngineVersionReaderService.getDatabaseServerMajorVersion(sdxCluster)).thenReturn(Optional.empty());

        boolean upgradeNeeded = underTest.isUpgradeNeeded(sdxCluster, targetMajorVersion);

        assertTrue(upgradeNeeded);
    }

}