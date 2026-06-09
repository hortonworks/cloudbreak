package com.sequenceiq.cloudbreak.service.upgrade;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.upgrade.validation.service.ServiceUpgradeValidationRequest;

public final class ServiceUpgradeValidationRequestTestUtils {

    private ServiceUpgradeValidationRequestTestUtils() {
    }

    public static ServiceUpgradeValidationRequest of(StackDto stack, ClusterUpgradeProperties clusterUpgradeProperties) {
        return new ServiceUpgradeValidationRequest(
                stack,
                clusterUpgradeProperties.isLockComponents(),
                clusterUpgradeProperties.isRollingUpgradeEnabled(),
                null,
                clusterUpgradeProperties,
                clusterUpgradeProperties.isReplaceVms());
    }
}
