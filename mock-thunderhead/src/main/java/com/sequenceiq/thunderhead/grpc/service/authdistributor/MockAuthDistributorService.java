package com.sequenceiq.thunderhead.grpc.service.authdistributor;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorGrpc.AuthDistributorImplBase;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentRequest;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.FetchAuthViewForEnvironmentResponse;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentRequest;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.RemoveAuthViewForEnvironmentResponse;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentRequest;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UpdateAuthViewForEnvironmentResponse;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.sequenceiq.thunderhead.service.UserStateStoreService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Component
public class MockAuthDistributorService extends AuthDistributorImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockAuthDistributorService.class);

    @Inject
    private UserStateStoreService userStateStoreService;

    @Override
    public void updateAuthViewForEnvironment(UpdateAuthViewForEnvironmentRequest request,
            StreamObserver<UpdateAuthViewForEnvironmentResponse> responseObserver) {
        LOGGER.info("Update auth view for environment: {}", request.getEnvironmentCrn());
        userStateStoreService.store(request.getEnvironmentCrn(), request.getUserState());
        responseObserver.onNext(UpdateAuthViewForEnvironmentResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void removeAuthViewForEnvironment(RemoveAuthViewForEnvironmentRequest request,
            StreamObserver<RemoveAuthViewForEnvironmentResponse> responseObserver) {
        LOGGER.info("Remove auth view for environment: {}", request.getEnvironmentCrn());
        userStateStoreService.remove(request.getEnvironmentCrn());
        responseObserver.onNext(RemoveAuthViewForEnvironmentResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchAuthViewForEnvironment(FetchAuthViewForEnvironmentRequest request,
            StreamObserver<FetchAuthViewForEnvironmentResponse> responseObserver) {
        UserState userState = userStateStoreService.fetch(request.getEnvironmentCrn());
        if (userState == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("User state not found for environment: " + request.getEnvironmentCrn())
                    .asRuntimeException());
        } else {
            FetchAuthViewForEnvironmentResponse response = FetchAuthViewForEnvironmentResponse.newBuilder()
                    .setUserState(userState)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
