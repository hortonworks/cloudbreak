package com.sequenceiq.cloudbreak.idbmms;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.altus.RequestIdUtil;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.idbmms.config.IdbmmsConfig;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationException;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentracing.Tracer;

/**
 * A GRPC-based client for the IDBroker Mapping Management Service (IDBMMS).
 */
@Component
public class GrpcIdbmmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcIdbmmsClient.class);

    @Inject
    private IdbmmsConfig idbmmsConfig;

    @Inject
    private Tracer tracer;

    public static GrpcIdbmmsClient createClient(IdbmmsConfig clientConfig, Tracer tracer) {
        GrpcIdbmmsClient client = new GrpcIdbmmsClient();
        client.idbmmsConfig = Preconditions.checkNotNull(clientConfig, "clientConfig should not be null.");
        client.tracer = Preconditions.checkNotNull(tracer, "tracer should not be null.");
        return client;
    }

    /**
     * Retrieves IDBroker mappings from IDBMMS for a particular environment.
     *
     * @param actorCrn       the actor CRN; must not be {@code null}
     * @param environmentCrn the environment CRN to get mappings for; must not be {@code null}
     * @param requestId      an optional request ID; must not be {@code null}
     * @return the mappings config associated with environment {@code environmentCrn}; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     * @throws IdbmmsOperationException if any problem is encountered during the IDBMMS call processing
     */
    public MappingsConfig getMappingsConfig(String actorCrn, String environmentCrn, Optional<String> requestId) {
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(environmentCrn);
        checkNotNull(requestId, "requestId should not be null.");
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
     * @param actorCrn       the actor CRN; must not be {@code null}
     * @param environmentCrn the environment CRN to delete mappings for; must not be {@code null}
     * @param requestId      an optional request ID; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     * @throws IdbmmsOperationException if any problem is encountered during the IDBMMS call processing
     */
    public void deleteMappings(String actorCrn, String environmentCrn, Optional<String> requestId) {
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(environmentCrn);
        checkNotNull(requestId, "requestId should not be null.");
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

    /**
     * Sets IDBroker mappings in IDBMMS for a particular environment.
     *
     * @param requestId      an optional request ID; must not be {@code null}
     * @param actorCrn       the actor CRN; must not be {@code null}
     * @param environmentCrn the environment CRN to delete mappings for; must not be {@code null}
     * @param dataAccessRole the cloud provider role to which data access services will be mapped
     * @param baselineRole   the cloud provider role associated with the baseline instance identity,
     *                       that write to cloud storage will be mapped to this role.
     * @throws NullPointerException if either argument is {@code null}
     * @throws IdbmmsOperationException if any problem is encountered during the IDBMMS call processing
     */
    public IdBrokerMappingManagementProto.SetMappingsResponse setMappings(String actorCrn, String environmentCrn, String dataAccessRole, String baselineRole,
            Optional<String> requestId) {
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(environmentCrn, "environment Crn should not be null.");
        checkNotNull(dataAccessRole, "data access role should not be null.");
        checkNotNull(baselineRole, "baseline role should not be null.");

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            IdbmmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            String effectiveRequestId = RequestIdUtil.getOrGenerate(MDCUtils.getRequestId());
            String accountId = Crn.fromString(environmentCrn).getAccountId();
            LOGGER.debug("Setting IDBroker mappings for environment {} using request ID {}", environmentCrn, effectiveRequestId);
            IdBrokerMappingManagementProto.SetMappingsResponse mappings = client.setMappings(effectiveRequestId, accountId, environmentCrn,
                    dataAccessRole, baselineRole);
            LOGGER.debug("Set IDBroker mappings for environment {}", environmentCrn);
            return mappings;
        } catch (RuntimeException e) {
            throw new IdbmmsOperationException(String.format("Error during IDBMMS operation: %s", e.getMessage()), e);
        }
    }

    /**
     * Retrieves IDBroker mappings from IDBMMS for a particular environment.
     *
     * @param actorCrn       the actor CRN; must not be {@code null}
     * @param environmentCrn the environment CRN to get mappings for; must not be {@code null}
     * @param requestId      an optional request ID; must not be {@code null}
     * @return the mappings config associated with environment {@code environmentCrn}; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     * @throws IdbmmsOperationException if any problem is encountered during the IDBMMS call processing
     */
    public IdBrokerMappingManagementProto.GetMappingsResponse getMappings(String actorCrn, String environmentCrn, Optional<String> requestId) {
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(environmentCrn, "actorCrn should not be null.");

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            IdbmmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            String effectiveRequestId = RequestIdUtil.getOrGenerate(MDCUtils.getRequestId());
            String accountId = Crn.fromString(environmentCrn).getAccountId();
            LOGGER.debug("Fetching IDBroker mappings for environment {} using request ID {}", environmentCrn, effectiveRequestId);
            IdBrokerMappingManagementProto.GetMappingsResponse mappings = client.getMappings(effectiveRequestId, accountId, environmentCrn);
            LOGGER.debug("Retrieved IDBroker mappings for environment {}", environmentCrn);
            return mappings;
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
        return new IdbmmsClient(channel, actorCrn, tracer);
    }

}
