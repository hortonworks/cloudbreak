package com.sequenceiq.cloudbreak.idbmms;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementGrpc;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementGrpc.IdBrokerMappingManagementBlockingStub;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsRequest;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.DeleteMappingsResponse;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigRequest;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsConfigResponse;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsRequest;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.GetMappingsResponse;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsRequest.Builder;
import com.cloudera.thunderhead.service.idbrokermappingmanagement.IdBrokerMappingManagementProto.SetMappingsResponse;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.altus.CallingServiceNameInterceptor;
import com.sequenceiq.cloudbreak.idbmms.config.IdbmmsClientConfig;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

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

    private final IdbmmsClientConfig idbmmsClientConfig;

    IdbmmsClient(ManagedChannel channel, IdbmmsClientConfig idbmmsClientConfig, String actorCrn) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.idbmmsClientConfig = checkNotNull(idbmmsClientConfig, "clientConfig should not be null.");
        this.actorCrn = checkNotNull(actorCrn, "actorCrn should not be null.");
    }

    private static String getOrGenerate(Optional<String> requestId) {
        return requestId.orElse(MDCBuilder.getOrGenerateRequestId());
    }

    private IdBrokerMappingManagementBlockingStub newStub() {
        String requestId = getOrGenerate(MDCUtils.getRequestId());
        return IdBrokerMappingManagementGrpc.newBlockingStub(channel)
                .withInterceptors(
                        new AltusMetadataInterceptor(requestId, actorCrn),
                        new CallingServiceNameInterceptor(idbmmsClientConfig.getCallingServiceName()));
    }

    /**
     * Wraps a call to {@code GetMappingsConfig}.
     *
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @return the mappings config; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    MappingsConfig getMappingsConfig(String environmentCrn) {
        checkNotNull(environmentCrn, "environment Crn should not be null.");
        GetMappingsConfigRequest request = GetMappingsConfigRequest.newBuilder()
                .setEnvironmentCrn(environmentCrn)
                .build();
        GetMappingsConfigResponse mappingsConfigResponse = newStub().getMappingsConfig(request);
        return new MappingsConfig(mappingsConfigResponse.getMappingsVersion(), mappingsConfigResponse.getActorMappingsMap(),
                mappingsConfigResponse.getGroupMappingsMap());
    }

    /**
     * Wraps a call to {@code DeleteMappings}.
     *
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @throws NullPointerException if either argument is {@code null}
     * @return delete mappings response
     */
    DeleteMappingsResponse deleteMappings(String environmentCrn) {
        checkNotNull(environmentCrn, "environment Crn should not be null.");
        DeleteMappingsRequest request = DeleteMappingsRequest.newBuilder()
                        .setEnvironmentCrn(environmentCrn)
                        .build();
        return newStub().deleteMappings(request);
    }

    /**
     * Wraps a call to {@code CreateMappings}.
     *
     * @param environmentCrn             the environment CRN; must not be {@code null}
     * @param dataAccessRole             the cloud provider role to which data access services will be mapped; must not be {@code null}
     * @param baselineRole               the cloud provider role associated with the baseline instance identity,
     *                                   that write to cloud storage will be mapped to this role; must not be {@code null}
     * @param rangerAccessAuthorizerRole the cloud provider role to which the Ranger RAZ service will be mapped
     * @return the set mappings response; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    SetMappingsResponse setMappings(String environmentCrn, String dataAccessRole, String baselineRole, String rangerAccessAuthorizerRole) {
        checkNotNull(environmentCrn, "environment Crn should not be null.");
        checkNotNull(dataAccessRole, "data access role should not be null.");
        checkNotNull(baselineRole, "baseline role should not be null.");

        Builder setMappingsRequestBuilder = SetMappingsRequest.newBuilder()
                .setBaselineRole(baselineRole)
                .setDataAccessRole(dataAccessRole)
                .setEnvironmentNameOrCrn(environmentCrn)
                .setAccountId(Crn.safeFromString(environmentCrn).getAccountId());
        if (StringUtils.isNotBlank(rangerAccessAuthorizerRole)) {
            setMappingsRequestBuilder.setRangerCloudAccessAuthorizerRole(rangerAccessAuthorizerRole);
        }
        SetMappingsRequest request = setMappingsRequestBuilder.build();
        return newStub().setMappings(request);
    }

    /**
     * Wraps a call to {@code GetMappings}.
     *
     * @param environmentCrn the environment CRN; must not be {@code null}
     * @return the get mappings response; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    GetMappingsResponse getMappings(String environmentCrn) {
        checkNotNull(environmentCrn, "environment Crn should not be null.");

        GetMappingsRequest request = GetMappingsRequest.newBuilder()
                .setEnvironmentNameOrCrn(environmentCrn)
                .setAccountId(Crn.safeFromString(environmentCrn).getAccountId())
                .build();
        return newStub().getMappings(request);
    }
}
