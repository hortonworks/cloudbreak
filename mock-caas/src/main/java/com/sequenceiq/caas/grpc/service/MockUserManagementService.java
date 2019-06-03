package com.sequenceiq.caas.grpc.service;

import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse;
import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest;
import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse;
import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import static org.springframework.security.jwt.JwtHelper.decodeAndVerify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.sequenceiq.caas.grpc.GrpcActorContext;
import com.sequenceiq.caas.model.AltusToken;
import com.sequenceiq.caas.util.CrnHelper;
import com.sequenceiq.caas.util.JsonUtil;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

import io.grpc.stub.StreamObserver;

@Service
public class MockUserManagementService extends UserManagementGrpc.UserManagementImplBase {

    public static final MacSigner SIGNATURE_VERIFIER = new MacSigner("titok");

    private static final Logger LOG = LoggerFactory.getLogger(MockUserManagementService.class);

    @Inject
    private JsonUtil jsonUtil;

    @Value("#{'${auth.config.dir:}/${auth.license.file:}'}")
    private String cbLicenseFilePath;

    private String cbLicense;

    @PostConstruct
    public void setLicense() {
        this.cbLicense = getLicense();
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        String userIdOrCrn = request.getUserIdOrCrn();
        String[] splittedCrn = userIdOrCrn.split(":");
        String userName = splittedCrn[6];
        String accountId = splittedCrn[4];
        responseObserver.onNext(
                GetUserResponse.newBuilder()
                        .setUser(createUser(accountId, userName))
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listUsers(UserManagementProto.ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        if (request.getUserIdOrCrnCount() == 0) {
            responseObserver.onNext(ListUsersResponse.newBuilder().build());
        } else {
            String userIdOrCrn = request.getUserIdOrCrn(0);
            String[] splittedCrn = userIdOrCrn.split(":");
            String userName = splittedCrn[6];
            String accountId = splittedCrn[4];
            responseObserver.onNext(
                    ListUsersResponse.newBuilder()
                            .addUser(createUser(accountId, userName))
                            .build());
        }
        responseObserver.onCompleted();
    }

    private User createUser(String accountId, String userName) {
        return User.newBuilder()
                .setUserId(UUID.nameUUIDFromBytes((accountId + "#" + userName).getBytes()).toString())
                .setCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                .setEmail(userName)
                .build();
    }

    @Override
    public void listMachineUsers(UserManagementProto.ListMachineUsersRequest request, StreamObserver<ListMachineUsersResponse> responseObserver) {
        if (request.getMachineUserNameOrCrnCount() == 0) {
            responseObserver.onNext(ListMachineUsersResponse.newBuilder().build());
        } else {
            String machineUserIdOrCrn = request.getMachineUserNameOrCrn(0);
            String[] splittedCrn = machineUserIdOrCrn.split(":");
            String userName;
            String accountId;
            if (splittedCrn.length > 1) {
                userName = splittedCrn[6];
                accountId = splittedCrn[4];
            } else {
                userName = machineUserIdOrCrn;
                accountId = UUID.randomUUID().toString();
            }
            responseObserver.onNext(
                    ListMachineUsersResponse.newBuilder()
                            .addMachineUser(MachineUser.newBuilder()
                                    .setMachineUserId(UUID.nameUUIDFromBytes((accountId + "#" + userName).getBytes()).toString())
                                    .setCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                                    .build())
                            .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
        responseObserver.onNext(
                GetAccountResponse.newBuilder()
                        .setAccount(UserManagementProto.Account.newBuilder()
                                .setClouderaManagerLicenseKey(cbLicense)
                                .build())
                        .build());
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

    public void assignResourceRole(UserManagementProto.AssignResourceRoleRequest request,
            StreamObserver<UserManagementProto.AssignResourceRoleResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.AssignResourceRoleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void unassignResourceRole(UserManagementProto.UnassignResourceRoleRequest request,
            StreamObserver<UserManagementProto.UnassignResourceRoleResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.UnassignResourceRoleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void assignRole(UserManagementProto.AssignRoleRequest request, StreamObserver<UserManagementProto.AssignRoleResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.AssignRoleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void unassignRole(UserManagementProto.UnassignRoleRequest request, StreamObserver<UserManagementProto.UnassignRoleResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.UnassignRoleResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAssigneeAuthorizationInformation(UserManagementProto.GetAssigneeAuthorizationInformationRequest request,
            StreamObserver<UserManagementProto.GetAssigneeAuthorizationInformationResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.GetAssigneeAuthorizationInformationResponse.newBuilder()
                .setResourceAssignment(0, createResourceAssigment(request.getAssigneeCrn()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listResourceAssignees(UserManagementProto.ListResourceAssigneesRequest request,
            StreamObserver<UserManagementProto.ListResourceAssigneesResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.ListResourceAssigneesResponse.newBuilder()
                .setResourceAssignee(0, createResourceAssignee(request.getResourceCrn()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void notifyResourceDeleted(UserManagementProto.NotifyResourceDeletedRequest request,
            StreamObserver<UserManagementProto.NotifyResourceDeletedResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.NotifyResourceDeletedResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void createAccessKey(UserManagementProto.CreateAccessKeyRequest request,
            StreamObserver<UserManagementProto.CreateAccessKeyResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.CreateAccessKeyResponse.newBuilder()
                .setPrivateKey(UUID.randomUUID().toString())
                .setAccessKey(UserManagementProto.AccessKey.newBuilder()
                        .setAccessKeyId(UUID.randomUUID().toString())
                        .build())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listAccessKeys(UserManagementProto.ListAccessKeysRequest request, StreamObserver<UserManagementProto.ListAccessKeysResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.ListAccessKeysResponse.newBuilder()
                .addAccessKey(0, UserManagementProto.AccessKey.newBuilder()
                        .setAccessKeyId(UUID.randomUUID().toString())
                        .setCrn(UUID.randomUUID().toString())
                        .build())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteAccessKey(UserManagementProto.DeleteAccessKeyRequest request,
            StreamObserver<UserManagementProto.DeleteAccessKeyResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.DeleteAccessKeyResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void createMachineUser(UserManagementProto.CreateMachineUserRequest request,
            StreamObserver<UserManagementProto.CreateMachineUserResponse> responseObserver) {
        String accountId = Crn.fromString(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn()).getAccountId();
        String name = request.getMachineUserName();
        responseObserver.onNext(UserManagementProto.CreateMachineUserResponse.newBuilder()
                .setMachineUser(MachineUser.newBuilder()
                        .setMachineUserId(UUID.nameUUIDFromBytes((accountId + "#" + name).getBytes()).toString())
                        .setCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                        .build())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteMachineUser(UserManagementProto.DeleteMachineUserRequest request,
            StreamObserver<UserManagementProto.DeleteMachineUserResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.DeleteMachineUserResponse.newBuilder()
                .build());
        responseObserver.onCompleted();
    }

    private String getLicense() {
        String license = "";
        try {
            if (Files.exists(Paths.get(cbLicenseFilePath))) {
                LOG.info("Cloudbreak license file successfully loaded.");
                license = Files.readString(Path.of(cbLicenseFilePath));
            } else {
                LOG.warn("The license file is not exists in path: {}", cbLicenseFilePath);
            }
        } catch (IOException e) {
            LOG.warn("Error during reading license.", e);
        }
        return license;
    }

    private UserManagementProto.ResourceAssignee createResourceAssignee(String resourceCrn) {
        return UserManagementProto.ResourceAssignee.newBuilder()
                .setAssigneeCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                .setResourceRoleCrn(createCrn(resourceCrn, Crn.ResourceType.RESOURCE_ROLE, "WorkspaceManager"))
                .build();
    }

    private UserManagementProto.ResourceAssignment createResourceAssigment(String assigneeCrn) {
        String resourceCrn = createCrn(assigneeCrn, Crn.ResourceType.WORKSPACE, Crn.fromString(assigneeCrn).getAccountId());
        return UserManagementProto.ResourceAssignment.newBuilder()
                .setResourceCrn(resourceCrn)
                .setResourceRoleCrn(createCrn(assigneeCrn, Crn.ResourceType.RESOURCE_ROLE, "WorkspaceManager"))
                .build();
    }

    private String createCrn(String baseCrn, Crn.ResourceType resourceType, String resource) {
        return Crn.builder()
                .setAccountId(Crn.fromString(baseCrn).getAccountId())
                .setService(Crn.fromString(baseCrn).getService())
                .setResourceType(resourceType)
                .setResource(resource)
                .build()
                .toString();
    }
}