package com.sequenceiq.datalake.service.recovery;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.UpgradeRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

/**
 * Defines a service that can be used to recover CB deployed infrastructure.
 *
 * Services that can recovery failed infrastructure must be able to <em>validate</em> that a recoveyr action is appropriate and
 * be able to <em>trigger</em> the recovery.
 *
 * The Recovery Service is parameterized by the type of recovery request made to it. The recovery request should be a value object representing
 * an HTTP response object.
 * @param <T> entity representing additional properties for the recovery
 */
public interface RecoveryService<T extends UpgradeRecoveryRequest> {
    /**
     * Starts recovery of the CB infrastructure related to the recovery request.
     *
     * This should validate that recovery is appropriate before triggerng the recovery.
     *
     * The {@code <T>} type carries additional information about the recovery, it may be considered optional.
     *
     * Trigger recovery may start a Flow or FlowChain to perform the recovery operations.
     *
     * @param userCrn the CRN of the user requesting recovery
     * @param nameOrCrn the name or CRN of the resource to recover
     * @param upgradeRecoveryRequest detailed information about the recovery
     * @return a response containing the identifier of the triggered recovery Flow
     */
    SdxRecoveryResponse triggerRecovery(String userCrn, NameOrCrn nameOrCrn, T upgradeRecoveryRequest);

    /**
     * Validates that triggering a recovery is allowed.
     *
     * Recovery validation should be used in {@code triggerRecovery} to make sure that a particular recovery is still allowed before it's triggered.
     *
     * @param userCrn the CRN of the user requesting recovery validation
     * @param nameOrCrn the name or CRN of the resource to validate
     * @return a message detailing if recovery is allowed and the reason why
     */
    SdxRecoverableResponse validateRecovery(String userCrn, NameOrCrn nameOrCrn);
}
