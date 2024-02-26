package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

public record ServiceUpgradeValidationRequest(
        StackDto stack,
        boolean lockComponents,
        boolean rollingUpgradeEnabled,
        UpgradeImageInfo upgradeImageInfo) {
}
