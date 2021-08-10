package com.sequenceiq.cloudbreak.ccmimpl.termination;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Throwables;
import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccmimpl.altus.GrpcMinaSshdManagementClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

/**
 * Default CCM resource termination listener.
 */
@Component
public class DefaultCcmResourceTerminationListener implements CcmResourceTerminationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCcmResourceTerminationListener.class);

    @Inject
    private GrpcMinaSshdManagementClient grpcMinaSshdManagementClient;

    @Override
    public void deregisterCcmSshTunnelingKey(@Nonnull String actorCrn, @Nonnull String accountId, @Nonnull String keyId, String minaSshdServiceId) {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        try {
            grpcMinaSshdManagementClient.unregisterSshTunnelingKey(requestId, actorCrn, accountId, keyId, minaSshdServiceId);
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new RuntimeException(e);
        }
    }
}
