package com.sequenceiq.caas.grpc.service;

import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse;
import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest;
import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse;
import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import static com.sequenceiq.caas.service.MockCaasService.SIGNATURE_VERIFIER;
import static org.springframework.security.jwt.JwtHelper.decodeAndVerify;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.caas.grpc.GrpcActorContext;
import com.sequenceiq.caas.model.AltusToken;
import com.sequenceiq.caas.util.CrnHelper;
import com.sequenceiq.caas.util.JsonUtil;

import io.grpc.stub.StreamObserver;

@Service
public class MockUserManagementService extends UserManagementGrpc.UserManagementImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(MockUserManagementService.class);

    @Inject
    private JsonUtil jsonUtil;

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        String userIdOrCrn = request.getUserIdOrCrn();
        String[] splittedCrn = userIdOrCrn.split(":");
        String userName = splittedCrn[6];
        String accountId = splittedCrn[4];
        responseObserver.onNext(
                GetUserResponse.newBuilder()
                        .setUser(User.newBuilder()
                                .setUserId(UUID.nameUUIDFromBytes((accountId + "#" + userName).getBytes()).toString())
                                .setCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                                .setEmail(userName)
                                .build())
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
        responseObserver.onNext(GetAccountResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void verifyInteractiveUserSessionToken(UserManagementProto.VerifyInteractiveUserSessionTokenRequest request,
            StreamObserver<UserManagementProto.VerifyInteractiveUserSessionTokenResponse> responseObserver) {
        String sessionToken = request.getSessionToken();
        Jwt token = decodeAndVerify(sessionToken, SIGNATURE_VERIFIER);
        AltusToken introspectResponse = jsonUtil.toObject(token.getClaims(), AltusToken.class);
        String userIdOrCrn = introspectResponse.getSub();
        String[] splittedCrn = userIdOrCrn.split(":");
        responseObserver.onNext(
                UserManagementProto.VerifyInteractiveUserSessionTokenResponse.newBuilder()
                        .setAccountId(splittedCrn[4])
                        .setAccountType(UserManagementProto.AccountType.REGULAR)
                        .setUserCrn(userIdOrCrn)
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void authenticate(UserManagementProto.AuthenticateRequest request,
            StreamObserver<UserManagementProto.AuthenticateResponse> responseObserver) {
        String authHeader = request.getAccessKeyV1AuthRequest().getAuthHeader();
        String crn = CrnHelper.extractCrnFromAuthHeader(authHeader);
        LOG.info("Crn: {}", crn);

        responseObserver.onNext(
                UserManagementProto.AuthenticateResponse.newBuilder()
                    .setActorCrn(crn)
                    .build());
        responseObserver.onCompleted();
    }
}