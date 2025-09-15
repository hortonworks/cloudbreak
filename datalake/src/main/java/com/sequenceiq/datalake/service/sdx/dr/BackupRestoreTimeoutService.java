package com.sequenceiq.datalake.service.sdx.dr;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.datalake.entity.SdxBackupRestoreSettings;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Service
public class BackupRestoreTimeoutService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRestoreTimeoutService.class);

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private SdxService sdxService;

    @Inject
    private EntitlementService entitlementService;

    public int getRestoreTimeout(Long sdxId, int drMaxDuration, int defaultDuration, int entitlementBasedDuration) {
        SdxBackupRestoreSettings sdxBackupRestoreSettings = getSdxBackupRestoreSettings(sdxId);
        int customTimeoutDuration = sdxBackupRestoreSettings != null ? sdxBackupRestoreSettings.getRestoreTimeoutInMinutes() : 0;
        return evaluateTimeout(customTimeoutDuration, drMaxDuration, defaultDuration, entitlementBasedDuration);
    }

    public int getBackupTimeout(Long sdxId, int drMaxDuration, int defaultDuration, int entitlementBasedDuration) {
        SdxBackupRestoreSettings sdxBackupRestoreSettings = getSdxBackupRestoreSettings(sdxId);
        int customTimeoutDuration = sdxBackupRestoreSettings != null ? sdxBackupRestoreSettings.getBackupTimeoutInMinutes() : 0;
        return evaluateTimeout(customTimeoutDuration, drMaxDuration, defaultDuration, entitlementBasedDuration);
    }

    private SdxBackupRestoreSettings getSdxBackupRestoreSettings(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        return sdxBackupRestoreService.getSdxBackupRestoreSettings(sdxCluster);
    }

    private int evaluateTimeout(int customTimeoutDuration, int drMaxDuration, int defaultDuration, int entitlementBasedDuration) {
        LOGGER.info("Evaluating timeout with parameters: customTimeoutDuration: {}, drMaxDuration: {}, defaultDuration: {}, entitlementBasedDuration: {}",
                customTimeoutDuration, drMaxDuration, defaultDuration, entitlementBasedDuration);
        if (!entitlementService.isLongTimeBackupEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            entitlementBasedDuration = 0;
        }
        if (customTimeoutDuration > 0) {
            return customTimeoutDuration;
        } else if (entitlementBasedDuration > 0) {
            return entitlementBasedDuration;
        } else {
            return Math.max(drMaxDuration, defaultDuration);
        }
    }
}
