package com.sequenceiq.caas.grpc.service;

import static com.google.common.base.Preconditions.checkArgument;

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

    @Override
    public void checkRight(CheckRightRequest request, StreamObserver<CheckRightResponse> responseObserver) {
        checkArgument(!Strings.isNullOrEmpty(request.getActorCrn()));
        checkArgument(!Strings.isNullOrEmpty(request.getCheck().getRight()));

        responseObserver.onNext(CheckRightResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void hasRights(HasRightsRequest request, StreamObserver<HasRightsResponse> responseObserver) {
        checkArgument(!Strings.isNullOrEmpty(request.getActorCrn()));
        checkArgument(request.getCheckCount() > 0);

        HasRightsResponse.Builder builder = HasRightsResponse.newBuilder();
        request.getCheckList().forEach(check -> builder.addResult(true));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
