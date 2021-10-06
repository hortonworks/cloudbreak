package com.sequenceiq.datalake.service.resize.recovery;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.sdx.api.model.SdxRecoverableResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;

@Service
/**
 * Provides entrypoint for recovery of failed SDX resize.
 *
 * The main entry point is {@code triggerRecovery}, which starts a cloudbreak Flow to recover the Data Lake.
 * To ensure a Resize recovery is appropriate, use one of {@code canRecover} or {@code validateRecovery}.
 */
public class ResizeRecoveryService {

    /**
     * Determines if it's possible to run a <em>Resize</em> recovery, returning a simple boolean indicator
     * @return true if resize recovery is possible.
     */
    public boolean canRecover() {
        return false;
    }

    /**
     * Determines if it's possible to run a <em>Resize</em> recovery, returning a detailed validation message.
     *
     * This is distinguished from {@code canRecover} by including a detailed validation message, usable as a web response.
     * @return detailed validation message, as a web response
     */
    public SdxRecoverableResponse validateRecovery() {
        // todo: implement me
        throw new BadRequestException("SDX Resize Recovery is not yet implemented");
    }

    /**
     * Checks if recovery is possible, then performs the resize recovery if appropriate.
     * @return a response containing information for the triggered recovery Flow.
     */
    public SdxRecoveryResponse triggerRecovery() {
        validateRecovery();
        // todo: implement me
        return null;
    }

}
