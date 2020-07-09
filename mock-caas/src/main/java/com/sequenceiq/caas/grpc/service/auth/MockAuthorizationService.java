package com.sequenceiq.caas.grpc.service.auth;

import static com.google.common.base.Preconditions.checkArgument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.authorization.AuthorizationGrpc;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse;
import com.google.common.base.Strings;

import io.grpc.stub.StreamObserver;

@Service
public class MockAuthorizationService extends AuthorizationGrpc.AuthorizationImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockAuthorizationService.class);

    @Override
    public void checkRight(CheckRightRequest request, StreamObserver<CheckRightResponse> responseObserver) {
        LOGGER.info("Check {} right for {}, ", request.getCheck(), request.getActorCrn());
        checkArgument(!Strings.isNullOrEmpty(request.getActorCrn()));
        checkArgument(!Strings.isNullOrEmpty(request.getCheck().getRight()));

        responseObserver.onNext(CheckRightResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void hasRights(HasRightsRequest request, StreamObserver<HasRightsResponse> responseObserver) {
        LOGGER.info("Has rights for {}, right count: {}, ", request.getActorCrn(), request.getCheckCount());
        checkArgument(!Strings.isNullOrEmpty(request.getActorCrn()));
        checkArgument(request.getCheckCount() > 0);

        HasRightsResponse.Builder builder = HasRightsResponse.newBuilder();
        request.getCheckList().forEach(check -> {
            LOGGER.info("Add result true for {}", check.getRight());
            builder.addResult(true);
        });
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
