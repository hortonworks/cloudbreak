package com.sequenceiq.cloudbreak.idbmms;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementGrpc;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;

import io.grpc.ManagedChannel;

/**
 * <p>
 *     A simple wrapper to the GRPC IDBroker Mapping Management Service. This handles setting up
 *     the appropriate context-propagating interceptors and hides some boilerplate.
 * </p>
 *
 * <p>
 *     This class is meant to be used only by {@link GrpcIdbmmsClient}.
 * </p>
 */
class IdbmmsClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param actorCrn the actor CRN.
     */
    IdbmmsClient(ManagedChannel channel, String actorCrn) {
        this.channel = checkNotNull(channel);
        this.actorCrn = checkNotNull(actorCrn);
    }

    /**
     * Wraps a call to {@code GetMappingsConfig}.
     *
     * @param requestId the request ID for the request; must not be {@code null}
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @return the mappings config; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    MappingsConfig getMappingsConfig(String requestId, String environmentCrn) {
        checkNotNull(requestId);
        checkNotNull(environmentCrn);
        IdBrokerMappingManagementProto.GetMappingsConfigResponse mappingsConfig = newStub(requestId).getMappingsConfig(
                IdBrokerMappingManagementProto.GetMappingsConfigRequest.newBuilder()
                        .setEnvironmentCrn(environmentCrn)
                        .build()
        );
        long mappingsVersion = mappingsConfig.getMappingsVersion();
        Map<String, String> actorMappings = mappingsConfig.getActorMappingsMap();
        Map<String, String> groupMappings = mappingsConfig.getGroupMappingsMap();
        return new MappingsConfig(mappingsVersion, actorMappings, groupMappings);
    }

    /**
     * Wraps a call to {@code DeleteMappings}.
     *
     * @param requestId the request ID for the request; must not be {@code null}
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    void deleteMappings(String requestId, String environmentCrn) {
        checkNotNull(requestId);
        checkNotNull(environmentCrn);
        newStub(requestId).deleteMappings(
                IdBrokerMappingManagementProto.DeleteMappingsRequest.newBuilder()
                        .setEnvironmentCrn(environmentCrn)
                        .build()
        );
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private IdBrokerMappingManagementGrpc.IdBrokerMappingManagementBlockingStub newStub(String requestId) {
        checkNotNull(requestId);
        return IdBrokerMappingManagementGrpc.newBlockingStub(channel)
                .withInterceptors(new AltusMetadataInterceptor(requestId, actorCrn));
    }

}
