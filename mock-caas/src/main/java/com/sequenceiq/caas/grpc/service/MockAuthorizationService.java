package com.sequenceiq.caas.grpc.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.authorization.AuthorizationGrpc;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightResponse;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Service
public class MockAuthorizationService extends AuthorizationGrpc.AuthorizationImplBase {

    private static final Map<String, List<String>> MOCK_AUTHZ_USER_RIGHTS = Map.of(
            "datalakeuser@cloudera.com", Lists.newArrayList("datalake/read", "datahub/read", "datahub/write"),
            "datalakeadmin@cloudera.com", Lists.newArrayList("datalake/write", "datalake/read", "datahub/read", "datahub/write"),
            "datahubuser@cloudera.com", Lists.newArrayList("datahub/read"),
            "datahubadmin@cloudera.com", Lists.newArrayList("datahub/read", "datahub/write"));

    @Override
    public void checkRight(CheckRightRequest request, StreamObserver<CheckRightResponse> responseObserver) {
        String right = request.getCheck().getRight();

        checkArgument(!Strings.isNullOrEmpty(request.getActorCrn()));
        checkArgument(!Strings.isNullOrEmpty(right));

        String user = Crn.fromString(request.getActorCrn()).getResource();
        if (StringUtils.isBlank(request.getCheck().getResource()) ||
                !MOCK_AUTHZ_USER_RIGHTS.containsKey(user) ||
                MOCK_AUTHZ_USER_RIGHTS.get(user).contains(right)) {
            responseObserver.onNext(CheckRightResponse.newBuilder().build());
            responseObserver.onCompleted();
        } else {
            throw Status.INTERNAL
                    .withDescription(request.getActorCrn() + " has no right to perform " + right)
                    .asRuntimeException();
        }
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
