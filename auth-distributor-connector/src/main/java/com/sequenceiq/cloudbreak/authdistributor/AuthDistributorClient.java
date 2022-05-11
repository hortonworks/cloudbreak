package com.sequenceiq.cloudbreak.authdistributor;

import static com.google.common.base.Preconditions.checkNotNull;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorGrpc;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorGrpc.AuthDistributorBlockingStub;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.authdistributor.config.AuthDistributorConfig;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

public class AuthDistributorClient {

    private final ManagedChannel channel;

    private final AuthDistributorConfig authDistributorConfig;

    private final Tracer tracer;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AuthDistributorClient(ManagedChannel channel, AuthDistributorConfig authDistributorConfig, Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = channel;
        this.authDistributorConfig = authDistributorConfig;
        this.tracer = tracer;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public void updateAuthViewForEnvironment(String requestId, String environmentCrn, UserState userState) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(environmentCrn, "environmentCrn should not be null.");
        checkNotNull(userState, "userState should not be null.");

        UpdateAuthViewForEnvironmentRequest.Builder requestBuilder = UpdateAuthViewForEnvironmentRequest.newBuilder()
                .setEnvironmentCrn(environmentCrn)
                .setUserState(userState);

        newStub(requestId).updateAuthViewForEnvironment(requestBuilder.build());
    }

    public void removeAuthViewForEnvironment(String requestId, String environmentCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(environmentCrn, "environmentCrn should not be null.");

        RemoveAuthViewForEnvironmentRequest.Builder requestBuilder = RemoveAuthViewForEnvironmentRequest.newBuilder()
                .setEnvironmentCrn(environmentCrn);

        newStub(requestId).removeAuthViewForEnvironment(requestBuilder.build());
    }

    private AuthDistributorBlockingStub newStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        return AuthDistributorGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()));
    }
}