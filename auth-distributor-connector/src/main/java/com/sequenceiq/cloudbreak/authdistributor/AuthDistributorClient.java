package com.sequenceiq.cloudbreak.authdistributor;

import static com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorGrpc;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorGrpc.AuthDistributorBlockingStub;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.authdistributor.config.AuthDistributorConfig;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class AuthDistributorClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthDistributorClient.class);

    private final ManagedChannel channel;

    private AuthDistributorConfig authDistributorConfig;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AuthDistributorClient(ManagedChannel channel, AuthDistributorConfig authDistributorConfig,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = channel;
        this.authDistributorConfig = authDistributorConfig;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public void updateAuthViewForEnvironment(String environmentCrn, UserState userState) {
        checkNotNull(environmentCrn, "environmentCrn should not be null.");
        checkNotNull(userState, "userState should not be null.");

        UpdateAuthViewForEnvironmentRequest.Builder requestBuilder = UpdateAuthViewForEnvironmentRequest.newBuilder()
                .setEnvironmentCrn(environmentCrn)
                .setUserState(userState);

        newStub().updateAuthViewForEnvironment(requestBuilder.build());
    }

    public void removeAuthViewForEnvironment(String environmentCrn) {
        checkNotNull(environmentCrn, "environmentCrn should not be null.");

        RemoveAuthViewForEnvironmentRequest.Builder requestBuilder = RemoveAuthViewForEnvironmentRequest.newBuilder()
                .setEnvironmentCrn(environmentCrn);

        newStub().removeAuthViewForEnvironment(requestBuilder.build());
    }

    public Optional<UserState> fetchAuthViewForEnvironment(String environmentCrn) {
        checkNotNull(environmentCrn, "environmentCrn should not be null.");

        FetchAuthViewForEnvironmentRequest.Builder requestBuilder = FetchAuthViewForEnvironmentRequest.newBuilder()
                .setEnvironmentCrn(environmentCrn);

        try {
            FetchAuthViewForEnvironmentResponse response = newStub().fetchAuthViewForEnvironment(requestBuilder.build());
            return Optional.ofNullable(response.getUserState());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.NOT_FOUND.getCode())) {
                LOGGER.debug("Auth view for environment not found: {}", environmentCrn);
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    private AuthDistributorBlockingStub newStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return AuthDistributorGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(authDistributorConfig.getGrpcTimeoutSec()),
                        new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()));
    }
}