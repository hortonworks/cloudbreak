package com.sequenceiq.cloudbreak.idbmms;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementGrpc;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

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

    private final Tracer tracer;

    IdbmmsClient(ManagedChannel channel, String actorCrn, Tracer tracer) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.actorCrn = checkNotNull(actorCrn, "actorCrn should not be null.");
        this.tracer = tracer;
    }

    /**
     * Wraps a call to {@code GetMappingsConfig}.
     *
     * @param requestId      the request ID for the request; must not be {@code null}
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @return the mappings config; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    MappingsConfig getMappingsConfig(String requestId, String environmentCrn) {
        checkNotNull(requestId, "requestId should not be null.");
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
     * @param requestId      the request ID for the request; must not be {@code null}
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    void deleteMappings(String requestId, String environmentCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(environmentCrn);
        newStub(requestId).deleteMappings(
                IdBrokerMappingManagementProto.DeleteMappingsRequest.newBuilder()
                        .setEnvironmentCrn(environmentCrn)
                        .build()
        );
    }

    /**
     * Wraps a call to {@code CreateMappings}.
     *
     * @param requestId      the request ID for the request; must not be {@code null}
     * @param accountId      the account ID
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @param dataAccessRole the cloud provider role to which data access services will be mapped
     * @param baselineRole   the cloud provider role associated with the baseline instance identity,
     *                       that write to cloud storage will be mapped to this role.
     * @return the set mappings response; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    IdBrokerMappingManagementProto.SetMappingsResponse setMappings(String requestId, String accountId, String environmentCrn, String dataAccessRole,
            String baselineRole) {
        checkNotNull(requestId, "request Id should not be null.");
        checkNotNull(accountId, "account Id should not be null.");
        checkNotNull(environmentCrn, "environment Crn should not be null.");
        checkNotNull(dataAccessRole, "data access role should not be null.");
        checkNotNull(baselineRole, "baseline role should not be null.");

        IdBrokerMappingManagementProto.SetMappingsResponse setMappingsResponse = newStub(requestId).setMappings(
                IdBrokerMappingManagementProto.SetMappingsRequest.newBuilder()
                        .setAccountId(accountId)
                        .setEnvironmentNameOrCrn(environmentCrn)
                        .setDataAccessRole(dataAccessRole)
                        .setBaselineRole(baselineRole)
                        .build()
        );
        return setMappingsResponse;
    }

    /**
     * Wraps a call to {@code GetMappings}.
     *
     * @param requestId      the request ID for the request; must not be {@code null}
     * @param accountId      the account ID
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @return the get mappings response; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    IdBrokerMappingManagementProto.GetMappingsResponse getMappings(String requestId, String accountId, String environmentCrn) {
        checkNotNull(requestId, "request Id should not be null.");
        checkNotNull(accountId, "account Id should not be null.");
        checkNotNull(environmentCrn, "environment Crn should not be null.");

        IdBrokerMappingManagementProto.GetMappingsResponse getMappingsResponse = newStub(requestId).getMappings(
                IdBrokerMappingManagementProto.GetMappingsRequest.newBuilder()
                        .setAccountId(accountId)
                        .setEnvironmentNameOrCrn(environmentCrn)
                        .build()
        );
        return getMappingsResponse;
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private IdBrokerMappingManagementGrpc.IdBrokerMappingManagementBlockingStub newStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        return IdBrokerMappingManagementGrpc.newBlockingStub(channel)
                .withInterceptors(GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
