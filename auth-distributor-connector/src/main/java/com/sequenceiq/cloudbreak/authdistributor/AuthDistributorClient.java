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
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class AuthDistributorClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthDistributorClient.class);

    private final ManagedChannel channel;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AuthDistributorClient(ManagedChannel channel, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = channel;
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

    public Optional<UserState> fetchAuthViewForEnvironment(String requestId, String environmentCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(environmentCrn, "environmentCrn should not be null.");

        FetchAuthViewForEnvironmentRequest.Builder requestBuilder = FetchAuthViewForEnvironmentRequest.newBuilder()
                .setEnvironmentCrn(environmentCrn);

        try {
            FetchAuthViewForEnvironmentResponse response = newStub(requestId).fetchAuthViewForEnvironment(requestBuilder.build());
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

    private AuthDistributorBlockingStub newStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        return AuthDistributorGrpc.newBlockingStub(channel)
                .withInterceptors(
                        new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()));
    }
}