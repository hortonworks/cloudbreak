package com.sequenceiq.datalake.service.recovery;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.datalake.service.resize.recovery.ResizeRecoveryService;
import com.sequenceiq.datalake.service.upgrade.recovery.UpgradeRecoveryService;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.UpgradeRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@Service
/**
 * Recovers an SDX cluster from a failure during Resize or Upgrade.
 *
 * Chooses the appropriate recovery action to take based on the state of the SDX cluster.
 * <ul>
 *     <li>{@code SdxUpgradeRecoveryService}</li>
 *     <li>{@code ResizeRecoveryService}</li>
 * </ul>
 */
public class SdxRecoveryService implements RecoveryService<UpgradeRecoveryRequest> {
    @Inject
    private UpgradeRecoveryService upgradeRecoveryService;

    @Inject
    private ResizeRecoveryService resizeRecoveryService;

    // todo: refactor this to return a flow identifier
    @Override
    public SdxRecoveryResponse triggerRecovery(String userCrn, NameOrCrn nameOrCrn, UpgradeRecoveryRequest upgradeRecoveryRequest) {
        if (resizeRecoveryService.canRecover()) {
            return resizeRecoveryService.triggerRecovery(null, null, null);
        } else {
            return upgradeRecoveryService.triggerRecovery(userCrn, nameOrCrn, upgradeRecoveryRequest);
        }
    }

    @Override
    public SdxRecoverableResponse validateRecovery(String userCrn, NameOrCrn nameOrCrn) {
        if (resizeRecoveryService.canRecover()) {
            return resizeRecoveryService.validateRecovery(null, null);
        } else {
            return upgradeRecoveryService.validateRecovery(userCrn, nameOrCrn);
        }
    }
}
