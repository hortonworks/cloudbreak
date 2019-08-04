package com.sequenceiq.cloudbreak.idbmms;

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
 * A GRPC-based client for the IDBroker Mapping Management Service.
 */
@Component
public class GrpcIdbmmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcIdbmmsClient.class);

    @Inject
    private IdbmmsConfig idbmmsConfig;

    /**
     * Retrieves mappings from IDBMMS.
     *
     * @param actorCrn the actor CRN
     * @param environmentCrn the environment CRN to get mappings for
     * @param requestId an optional request ID
     * @return the mappings config associated with environment {@code environmentCrn}; never {@code null}
     */
    public MappingsConfig getMappingsConfig(String actorCrn, String environmentCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            IdbmmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Fetching mappings for environment {} using request ID {}", environmentCrn, requestId);
            MappingsConfig mappingsConfig = client.getMappingsConfig(requestId.orElse(UUID.randomUUID().toString()), environmentCrn);
            LOGGER.debug("Retrieved mappings of version {} for environment {}", mappingsConfig.getMappingsVersion(), environmentCrn);
            return mappingsConfig;
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
