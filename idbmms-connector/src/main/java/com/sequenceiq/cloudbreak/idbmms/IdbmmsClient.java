package com.sequenceiq.cloudbreak.idbmms;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementGrpc;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.idbmms.config.IdbmmsConfig;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;

/**
 * <p>
 * A simple wrapper to the GRPC IDBroker Mapping Management Service. This handles setting up
 * the appropriate context-propagating interceptors and hides some boilerplate.
 * </p>
 *
 * <p>
 * This class is meant to be used only by {@link GrpcIdbmmsClient}.
 * </p>
 */
class IdbmmsClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    private IdbmmsConfig idbmmsConfig;

    IdbmmsClient(ManagedChannel channel, String actorCrn, IdbmmsConfig idbmmsConfig) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.actorCrn = checkNotNull(actorCrn, "actorCrn should not be null.");
        this.idbmmsConfig = checkNotNull(idbmmsConfig, "idbmmsConfig should not be null.");
    }

    /**
     * Wraps a call to {@code GetMappingsConfig}.
     *
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @return the mappings config; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    MappingsConfig getMappingsConfig(String environmentCrn) {
        checkNotNull(environmentCrn);
        IdBrokerMappingManagementProto.GetMappingsConfigResponse mappingsConfig = newStub().getMappingsConfig(
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
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    void deleteMappings(String environmentCrn) {
        checkNotNull(environmentCrn);
        newStub().deleteMappings(
                IdBrokerMappingManagementProto.DeleteMappingsRequest.newBuilder()
                        .setEnvironmentCrn(environmentCrn)
                        .build()
        );
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @return the stub
     */
    private IdBrokerMappingManagementGrpc.IdBrokerMappingManagementBlockingStub newStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return IdBrokerMappingManagementGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(idbmmsConfig.getGrpcTimeoutSec()),
                        new AltusMetadataInterceptor(requestId, actorCrn));
    }

}
