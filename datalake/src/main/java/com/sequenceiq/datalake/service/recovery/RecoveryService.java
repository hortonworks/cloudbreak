package com.sequenceiq.datalake.service.recovery;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.datalake.service.resize.recovery.ResizeRecoveryService;
import com.sequenceiq.datalake.service.upgrade.recovery.SdxUpgradeRecoveryService;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@Service
/**
 * Recovers an SDX cluster from a failure during Resize or Upgrade.
 *
 * Chooses the appropriate recovery action to take based on the state of the SDX cluster.
 * <ul>
 *     <li>{@Code SdxUpgradeRecoveryService}</li>
 *     <li>{@Code ResizeRecoveryService}</li>
 * </ul>
 */
public class RecoveryService {
    @Inject
    private SdxUpgradeRecoveryService sdxUpgradeRecoveryService;

    @Inject
    private ResizeRecoveryService resizeRecoveryService;

    // todo: refactor this to return a flow identifier
    public SdxRecoveryResponse triggerRecovery(String crn, NameOrCrn nameOrCrn, SdxRecoveryRequest sdxRecoveryRequest) {
        if (resizeRecoveryService.canRecover()) {
            return resizeRecoveryService.triggerRecovery();
        } else {
            return sdxUpgradeRecoveryService.triggerRecovery(crn, nameOrCrn, sdxRecoveryRequest);
        }
    }

    public SdxRecoverableResponse validateRecovery(String crn, NameOrCrn nameOrCrn) {
        if (resizeRecoveryService.canRecover()) {
            return resizeRecoveryService.validateRecovery();
        } else {
            return sdxUpgradeRecoveryService.validateRecovery(crn, nameOrCrn);
        }
    }
}
