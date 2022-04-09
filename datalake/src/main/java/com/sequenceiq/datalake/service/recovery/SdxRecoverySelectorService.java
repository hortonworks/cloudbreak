package com.sequenceiq.datalake.service.recovery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@Service
/*
 * Recovers an SDX cluster from a failure during Resize or Upgrade.
 *
 * Chooses the appropriate RecoveryService to use based on the state of the SDX cluster.
 */
public class SdxRecoverySelectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRecoverySelectorService.class);

    private final List<RecoveryService> services;

    @Autowired
    public SdxRecoverySelectorService(List<RecoveryService> services) {
        this.services = services;
    }

    public SdxRecoveryResponse triggerRecovery(SdxCluster sdxCluster, SdxRecoveryRequest sdxRecoveryRequest) {
        List<String> responseReasons = new ArrayList<>();
        SdxRecoverableResponse recoverableResponse;

        for (RecoveryService recoveryService : services) {
            try {
                recoverableResponse = recoveryService.validateRecovery(sdxCluster, sdxRecoveryRequest);
            } catch (Exception e) {
                recoverableResponse = interpretValidationError(sdxCluster, e);
            }
            responseReasons.add(recoverableResponse.getReason());
            if (recoverableResponse.getStatus().recoverable()) {
                return recoveryService.triggerRecovery(sdxCluster, sdxRecoveryRequest);
            }
        }

        String reasons = String.join(", ", responseReasons);
        LOGGER.debug("Cluster is not in a recoverable state with message(s): {}", reasons);
        throw new BadRequestException(reasons);

    }

    public SdxRecoverableResponse validateRecovery(SdxCluster sdxCluster) {
        List<SdxRecoverableResponse> sdxRecoverableResponses = new ArrayList<>();
        SdxRecoverableResponse recoverableResponse;
        for (RecoveryService recoveryService : services) {
            try {
                recoverableResponse = recoveryService.validateRecovery(sdxCluster);
            } catch (Exception e) {
                recoverableResponse = interpretValidationError(sdxCluster, e);
            }
            sdxRecoverableResponses.add(recoverableResponse);
            if (recoverableResponse.getStatus().recoverable()) {
                return recoverableResponse;
            }
        }
        if (sdxRecoverableResponses.isEmpty()) {
            return new SdxRecoverableResponse("No valid Recovery Options available", RecoveryStatus.NON_RECOVERABLE);
        }
        if (sdxRecoverableResponses.size() == 1) {
            return sdxRecoverableResponses.get(0);
        }

        return new SdxRecoverableResponse(sdxRecoverableResponses.stream()
                .map(SdxRecoverableResponse::getReason).collect(Collectors.joining(", ")), RecoveryStatus.NON_RECOVERABLE);
    }

    private SdxRecoverableResponse interpretValidationError(SdxCluster sdxCluster, Exception e) {
        LOGGER.error("Stack recovery validation failed on cluster: " + sdxCluster.getClusterName(), e);
        return new SdxRecoverableResponse(String.format(
                "Stack recovery validation failed on cluster: [%s]. Message: [%s]", sdxCluster.getClusterName(),
                e.getMessage()), RecoveryStatus.NON_RECOVERABLE);
    }
}
