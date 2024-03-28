package com.sequenceiq.thunderhead.grpc.service.auth;

import static com.google.common.base.Preconditions.checkArgument;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.authorization.AuthorizationGrpc;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse;
import com.google.common.base.Strings;
import com.sequenceiq.thunderhead.grpc.service.auth.roles.MockEnvironmentUserResourceRole;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

@Service
public class MockAuthorizationService extends AuthorizationGrpc.AuthorizationImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockAuthorizationService.class);

    @Inject
    private MockEnvironmentUserResourceRole mockEnvironmentUserResourceRole;

    @Override
    public void checkRight(CheckRightRequest request, StreamObserver<CheckRightResponse> responseObserver) {
        LOGGER.info("Check {} right for {}, ", request.getCheck(), request.getActorCrn());
        checkArgument(!Strings.isNullOrEmpty(request.getActorCrn()));
        checkArgument(!Strings.isNullOrEmpty(request.getCheck().getRight()));

        if (mockEnvironmentUserResourceRole.hasRight(request.getActorCrn(), request.getCheck().getRight()) != MockPermissionControl.DENIED) {
            // by default everybody is a super admin in mock
            LOGGER.info("Check right succeeded for {} with right {}", request.getActorCrn(), request.getCheck().getRight());
            responseObserver.onNext(CheckRightResponse.newBuilder().build());
            responseObserver.onCompleted();
        } else {
            StatusRuntimeException statusRuntimeException = Status.PERMISSION_DENIED.withDescription(
                            String.format("Actor: '%s', right '%s'", request.getActorCrn(), request.getCheck().getRight()))
                    .asRuntimeException();
            LOGGER.info("Check right failed for {} with right {}", request.getActorCrn(), request.getCheck().getRight(), statusRuntimeException);
            responseObserver.onError(statusRuntimeException);
        }
    }

    @Override
    public void hasRights(HasRightsRequest request, StreamObserver<HasRightsResponse> responseObserver) {
        LOGGER.info("Has rights for {}, right count: {}, ", request.getActorCrn(), request.getCheckCount());
        checkArgument(!Strings.isNullOrEmpty(request.getActorCrn()));
        checkArgument(request.getCheckCount() > 0);

        HasRightsResponse.Builder builder = HasRightsResponse.newBuilder();
        request.getCheckList().forEach(check -> {
            LOGGER.info("Add result true for {}", check.getRight());
            // by default everybody is a super admin in mock
            builder.addResult(mockEnvironmentUserResourceRole.hasRight(request.getActorCrn(), check.getRight()) != MockPermissionControl.DENIED);
        });
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
