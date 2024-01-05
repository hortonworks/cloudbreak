package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.MinaSshdService;
import com.cloudera.thunderhead.service.minasshdmanagement.MinaSshdManagementProto.PublicKey;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CCMV1KeyRemapper;
import com.sequenceiq.cloudbreak.ccm.exception.CcmException;
import com.sequenceiq.cloudbreak.ccmimpl.altus.GrpcMinaSshdManagementClient;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component("DefaultCCMV1KeyRemapper")
public class DefaultCCMV1KeyRemapper implements CCMV1KeyRemapper  {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCCMV1KeyRemapper.class);

    @Inject
    private GrpcMinaSshdManagementClient grpcMinaSshdManagementClient;

    @Override
    public void remapKey(@Nonnull String actorCrn, @Nonnull String accountId, @Nonnull String originalKeyId,
            @Nonnull String newKeyId) throws CcmException, InterruptedException {
        LOGGER.info("Attempting to remap SSH tunneling key from key ID {} to new key ID {}", originalKeyId, newKeyId);
        String requestId = MDCBuilder.getOrGenerateRequestId();

        LOGGER.info("Attempting to acquire Mina SSHd service...");
        MinaSshdService minaSshdService = grpcMinaSshdManagementClient.acquireMinaSshdServiceAndWaitUntilReady(requestId, actorCrn, accountId);

        LOGGER.info("Attempting to get SSH tunneling key for key {}...", originalKeyId);
        PublicKey originalKey = grpcMinaSshdManagementClient.getSshTunnelingKey(
                requestId, actorCrn, minaSshdService.getMinaSshdServiceId(), originalKeyId
        );

        LOGGER.info("Attempting to register SSH tunneling key for new ID...");
        grpcMinaSshdManagementClient.registerSshTunnelingKey(
                requestId, actorCrn, accountId, minaSshdService.getMinaSshdServiceId(), newKeyId, originalKey
        );

        LOGGER.info("Attempting to remove SSH tunneling key for old key {}...", originalKeyId);
        grpcMinaSshdManagementClient.unregisterSshTunnelingKey(requestId, actorCrn, accountId, originalKeyId, minaSshdService.getMinaSshdServiceId());

        LOGGER.info("Finished remapping SSH tunneling key from key ID {} to new key ID {}", originalKeyId, newKeyId);
    }
}
