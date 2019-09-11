package com.sequenceiq.cloudbreak.idbmms;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.idbmms.config.IdbmmsConfig;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationException;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * A GRPC-based client for the IDBroker Mapping Management Service (IDBMMS).
 */
@Component
public class GrpcIdbmmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcIdbmmsClient.class);

    @Inject
    private IdbmmsConfig idbmmsConfig;

    /**
     * Retrieves IDBroker mappings from IDBMMS for a particular environment.
     *
     * @param actorCrn the actor CRN; must not be {@code null}
     * @param environmentCrn the environment CRN to get mappings for; must not be {@code null}
     * @param requestId an optional request ID; must not be {@code null}
     * @return the mappings config associated with environment {@code environmentCrn}; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     * @throws IdbmmsOperationException if any problem is encountered during the IDBMMS call processing
     */
    public MappingsConfig getMappingsConfig(String actorCrn, String environmentCrn, Optional<String> requestId) {
        checkNotNull(actorCrn);
        checkNotNull(environmentCrn);
        checkNotNull(requestId);
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            IdbmmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            String effectiveRequestId = requestId.orElse(UUID.randomUUID().toString());
            LOGGER.debug("Fetching IDBroker mappings for environment {} using request ID {}", environmentCrn, effectiveRequestId);
            MappingsConfig mappingsConfig = client.getMappingsConfig(effectiveRequestId, environmentCrn);
            LOGGER.debug("Retrieved IDBroker mappings of version {} for environment {}", mappingsConfig.getMappingsVersion(), environmentCrn);
            return mappingsConfig;
        } catch (RuntimeException e) {
            throw new IdbmmsOperationException(String.format("Error during IDBMMS operation: %s", e.getMessage()), e);
        }
    }

    /**
     * Deletes IDBroker mappings in IDBMMS for a particular environment.
     *
     * @param actorCrn the actor CRN; must not be {@code null}
     * @param environmentCrn the environment CRN to delete mappings for; must not be {@code null}
     * @param requestId an optional request ID; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     * @throws IdbmmsOperationException if any problem is encountered during the IDBMMS call processing
     */
    public void deleteMappings(String actorCrn, String environmentCrn, Optional<String> requestId) {
        checkNotNull(actorCrn);
        checkNotNull(environmentCrn);
        checkNotNull(requestId);
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            IdbmmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            String effectiveRequestId = requestId.orElse(UUID.randomUUID().toString());
            LOGGER.debug("Deleting IDBroker mappings for environment {} using request ID {}", environmentCrn, effectiveRequestId);
            client.deleteMappings(effectiveRequestId, environmentCrn);
            LOGGER.debug("Deleted IDBroker mappings for environment {}", environmentCrn);
        } catch (RuntimeException e) {
            throw new IdbmmsOperationException(String.format("Error during IDBMMS operation: %s", e.getMessage()), e);
        }
    }

    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(idbmmsConfig.getEndpoint(), idbmmsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }

    private IdbmmsClient makeClient(ManagedChannel channel, String actorCrn) {
        return new IdbmmsClient(channel, actorCrn);
    }

}
