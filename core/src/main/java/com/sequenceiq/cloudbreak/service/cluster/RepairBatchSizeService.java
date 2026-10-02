package com.sequenceiq.cloudbreak.service.cluster;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;

@Service
public class RepairBatchSizeService {

    @Value("${cb.upgrade.batch.repair.extended.nodecount:1000}")
    private int extendedBatchNodeCount;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    public int getBatchNodeCount(String accountId) {
        if (entitlementService.isExtendedRepairBatchSizeEnabled(accountId)) {
            return extendedBatchNodeCount;
        } else {
            return scalingHardLimitsService.getMaxUpscaleStepInNodeCount();
        }
    }
}
