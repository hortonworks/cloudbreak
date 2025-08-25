package com.sequenceiq.datalake.service.sdx.dr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.datalake.entity.SdxBackupRestoreSettings;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;

@ExtendWith(MockitoExtension.class)
class BackupRestoreTimeoutServiceTest {

    @Mock
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Mock
    private SdxService sdxService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private SdxBackupRestoreSettings sdxBackupRestoreSettings;

    @InjectMocks
    private BackupRestoreTimeoutService backupRestoreTimeoutService;

    @BeforeEach
    void setUp() {
        when(sdxService.getById(any(Long.class))).thenReturn(sdxCluster);
    }

    @Test
    void testGetBackupTimeoutReturnsCustomTimeoutWhenPositive() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getBackupTimeoutInMinutes()).thenReturn(60);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getBackupTimeout(1L, 30, 45, 90));

        assertThat(result).isEqualTo(60);
    }

    @Test
    void testGetBackupTimeoutReturnsEntitlementBasedDuration() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getBackupTimeoutInMinutes()).thenReturn(0);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(true);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getBackupTimeout(1L, 30, 45, 90));

        assertThat(result).isEqualTo(90);
    }

    @Test
    void testGetBackupTimeoutReturnsDrMaxDurationWhenGreaterThanDefault() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getBackupTimeoutInMinutes()).thenReturn(0);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getBackupTimeout(1L, 60, 30, 0));

        assertThat(result).isEqualTo(60);
    }

    @Test
    void testGetBackupTimeoutReturnsDefaultDurationWhenGreaterThanDrMax() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getBackupTimeoutInMinutes()).thenReturn(0);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getBackupTimeout(1L, 30, 60, 0));

        assertThat(result).isEqualTo(60);
    }

    @Test
    void testGetBackupTimeoutReturnsZeroWhenAllParametersAreZero() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getBackupTimeoutInMinutes()).thenReturn(0);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getBackupTimeout(1L, 0, 0, 0));

        assertThat(result).isEqualTo(0);
    }

    @Test
    void testGetBackupTimeoutReturnsCustomTimeoutWhenMinimum() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getBackupTimeoutInMinutes()).thenReturn(1);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(true);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getBackupTimeout(1L, 100, 200, 300));

        assertThat(result).isEqualTo(1);
    }

    @Test
    void testGetBackupTimeoutReturnsEntitlementBasedDurationWhenMinimum() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getBackupTimeoutInMinutes()).thenReturn(0);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(true);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getBackupTimeout(1L, 100, 200, 1));

        assertThat(result).isEqualTo(1);
    }

    @Test
    void testGetBackupTimeoutReturnsDrMaxDurationWhenSettingsIsNull() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(null);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getBackupTimeout(1L, 60, 30, 0));

        assertThat(result).isEqualTo(60);
    }

    @Test
    void testGetRestoreTimeoutReturnsCustomTimeoutWhenPositive() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getRestoreTimeoutInMinutes()).thenReturn(60);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getRestoreTimeout(1L, 30, 45, 90));

        assertThat(result).isEqualTo(60);
    }

    @Test
    void testGetRestoreTimeoutReturnsEntitlementBasedDuration() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getRestoreTimeoutInMinutes()).thenReturn(0);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(true);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getRestoreTimeout(1L, 30, 45, 90));

        assertThat(result).isEqualTo(90);
    }

    @Test
    void testGetRestoreTimeoutReturnsDrMaxDurationWhenGreaterThanDefault() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getRestoreTimeoutInMinutes()).thenReturn(0);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getRestoreTimeout(1L, 60, 30, 0));

        assertThat(result).isEqualTo(60);
    }

    @Test
    void testGetRestoreTimeoutReturnsDefaultDurationWhenGreaterThanDrMax() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getRestoreTimeoutInMinutes()).thenReturn(0);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getRestoreTimeout(1L, 30, 60, 0));

        assertThat(result).isEqualTo(60);
    }

    @Test
    void testGetRestoreTimeoutReturnsZeroWhenAllParametersAreZero() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getRestoreTimeoutInMinutes()).thenReturn(0);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getRestoreTimeout(1L, 0, 0, 0));

        assertThat(result).isEqualTo(0);
    }

    @Test
    void testGetRestoreTimeoutReturnsCustomTimeoutWhenMinimum() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getRestoreTimeoutInMinutes()).thenReturn(1);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getRestoreTimeout(1L, 100, 200, 300));

        assertThat(result).isEqualTo(1);
    }

    @Test
    void testGetRestoreTimeoutReturnsEntitlementBasedDurationWhenMinimum() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(sdxBackupRestoreSettings);
        when(sdxBackupRestoreSettings.getRestoreTimeoutInMinutes()).thenReturn(0);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(true);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getRestoreTimeout(1L, 100, 200, 1));

        assertThat(result).isEqualTo(1);
    }

    @Test
    void testGetRestoreTimeoutReturnsDrMaxDurationWhenSettingsIsNull() {
        when(sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster)).thenReturn(null);
        when(entitlementService.isLongTimeBackupEnabled(any(String.class))).thenReturn(false);

        int result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:test@example.com", () ->
                backupRestoreTimeoutService.getRestoreTimeout(1L, 60, 30, 0));

        assertThat(result).isEqualTo(60);
    }
}
