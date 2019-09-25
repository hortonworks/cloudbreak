package com.sequenceiq.caas.grpc.service;

import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.newSetFromMap;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.security.jwt.JwtHelper.decodeAndVerify;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse.Builder;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Policy;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.PolicyDefinition;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.PolicyStatement;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Role;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RoleAssignment;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.sequenceiq.caas.grpc.GrpcActorContext;
import com.sequenceiq.caas.model.AltusToken;
import com.sequenceiq.caas.util.CrnHelper;
import com.sequenceiq.caas.util.IniUtil;
import com.sequenceiq.caas.util.JsonUtil;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Service
public class MockUserManagementService extends UserManagementGrpc.UserManagementImplBase {

    private static final MacSigner SIGNATURE_VERIFIER = new MacSigner("titok");

    private static final Logger LOG = LoggerFactory.getLogger(MockUserManagementService.class);

    private static final int GROUPS_PER_ACCOUNT = 5;

    private static final int FIRST_GROUP = 0;

    private static final String ALTUS_ACCESS_KEY_ID = "altus_access_key_id";

    private static final String ALTUS_PRIVATE_KEY = "altus_private_key";

    private static final String CDP_ACCESS_KEY_ID = "cdp_access_key_id";

    private static final String CDP_PRIVATE_KEY = "cdp_private_key";

    private static final int MOCK_USER_COUNT = 10;

    private static final String ACCOUNT_ID_ALTUS = "altus";

    private static final long CREATION_DATE_MS = 1483228800000L;

    private static final String ALL_RIGHTS_AND_RESOURCES = "*";

    @Inject
    private JsonUtil jsonUtil;

    @Inject
    private IniUtil iniUtil;

    @Value("#{'${auth.config.dir:}/${auth.license.file:}'}")
    private String cmLicenseFilePath;

    @Value("#{'${auth.config.dir:}/${auth.databus.credential.tp.file:}'}")
    private String databusTpCredentialFile;

    @Value("#{'${auth.config.dir:}/${auth.databus.credential.fluent.file:}'}")
    private String databusFluentCredentialFile;

    @Value("${auth.databus.credential.tp.profile:default}")
    private String databusTpCredentialProfile;

    @Value("${auth.databus.credential.fluent.profile:default}")
    private String databusFluentCredentialProfile;

    private String cbLicense;

    private AltusCredential telemetyPublisherCredential;

    private AltusCredential fluentCredential;

    private final Map<String, Set<String>> accountUsers = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Group>> accountGroups = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.cbLicense = getLicense();
        this.telemetyPublisherCredential = getAltusCredential(databusTpCredentialFile, databusTpCredentialProfile);
        this.fluentCredential = getAltusCredential(databusFluentCredentialFile, databusFluentCredentialProfile);
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        String userIdOrCrn = request.getUserIdOrCrn();
        String[] splittedCrn = userIdOrCrn.split(":");
        String userName = splittedCrn[6];
        String accountId = splittedCrn[4];
        accountUsers.computeIfAbsent(accountId, key -> newSetFromMap(new ConcurrentHashMap<>())).add(userName);
        responseObserver.onNext(
                GetUserResponse.newBuilder()
                        .setUser(createUser(accountId, userName))
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        Builder userBuilder = ListUsersResponse.newBuilder();
        if (request.getUserIdOrCrnCount() == 0) {
            if (isNotEmpty(request.getAccountId())) {
                ofNullable(accountUsers.get(request.getAccountId())).orElse(Set.of()).stream()
                        .map(userName -> createUser(request.getAccountId(), userName))
                        .forEach(userBuilder::addUser);
                for (int i = 0; i < MOCK_USER_COUNT; i++) {
                    User user = createUser(request.getAccountId(), "fakeMockUser" + i);
                    userBuilder.addUser(user);
                }
            }
            responseObserver.onNext(userBuilder.build());
        } else {
            String userIdOrCrn = request.getUserIdOrCrn(0);
            String[] splittedCrn = userIdOrCrn.split(":");
            String userName = splittedCrn[6];
            String accountId = splittedCrn[4];
            responseObserver.onNext(
                    userBuilder
                            .addUser(createUser(accountId, userName))
                            .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getRights(GetRightsRequest request, StreamObserver<GetRightsResponse> responseObserver) {
        String actorCrn = request.getActorCrn();
        String accountId = Crn.fromString(actorCrn).getAccountId();
        List<Group> groups = List.copyOf(getOrCreateGroups(accountId));
        Group group = groups.get(FIRST_GROUP);
        PolicyStatement policyStatement = PolicyStatement.newBuilder()
                .addRight(ALL_RIGHTS_AND_RESOURCES)
                .addResource(ALL_RIGHTS_AND_RESOURCES)
                .build();
        PolicyDefinition policyDefinition = PolicyDefinition.newBuilder().addStatement(policyStatement).build();
        Policy powerUserPolicy = Policy.newBuilder()
                .setCrn(createCrn(ACCOUNT_ID_ALTUS, Crn.Service.IAM, Crn.ResourceType.POLICY, "PowerUserPolicy").toString())
                .setCreationDateMs(CREATION_DATE_MS)
                .setPolicyDefinition(policyDefinition)
                .build();
        Role powerUserRole = Role.newBuilder()
                .setCrn(createCrn(ACCOUNT_ID_ALTUS, Crn.Service.IAM, Crn.ResourceType.ROLE, "PowerUser").toString())
                .setCreationDateMs(CREATION_DATE_MS)
                .addPolicy(powerUserPolicy)
                .build();
        RoleAssignment roleAssignment = RoleAssignment.newBuilder().setRole(powerUserRole).build();
        GetRightsResponse.Builder responseBuilder = GetRightsResponse.newBuilder()
                .addGroupCrn(group.getCrn())
                .addRoleAssignment(roleAssignment)
                .addWorkloadAdministrationGroupName("mockworkloadadministrationgroup0");
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private User createUser(String accountId, String userName) {
        Crn actorCrn = Crn.safeFromString(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn());
        String userCrn = Crn.builder()
                .setService(actorCrn.getService())
                .setAccountId(actorCrn.getAccountId())
                .setResourceType(actorCrn.getResourceType())
                .setResource(userName)
                .build().toString();
        return User.newBuilder()
                .setUserId(UUID.nameUUIDFromBytes((accountId + "#" + userName).getBytes()).toString())
                .setCrn(userCrn)
                .setEmail(userName.contains("@") ? userName : userName + "@ums.mock")
                .setWorkloadUsername(sanitizeWorkloadUsername(userName))
                .build();
    }

    @Override
    public void listGroups(ListGroupsRequest request, StreamObserver<ListGroupsResponse> responseObserver) {

        ListGroupsResponse.Builder groupsBuilder = ListGroupsResponse.newBuilder();
        if (request.getGroupNameOrCrnCount() == 0) {
            if (isNotEmpty(request.getAccountId())) {
                getOrCreateGroups(request.getAccountId()).stream()
                        .forEach(groupsBuilder::addGroup);
            }
            responseObserver.onNext(groupsBuilder.build());
        } else {
            request.getGroupNameOrCrnList().stream()
                    .map(this::getOrCreateGroup)
                    .forEach(groupsBuilder::addGroup);
            responseObserver.onNext(groupsBuilder.build());
        }
        responseObserver.onCompleted();
    }

    private List<Group> getOrCreateGroups(String accountId) {
        accountGroups.computeIfAbsent(accountId, this::createGroups);
        List<Group> groups = new ArrayList<>(accountGroups.get(accountId).values());
        groups.sort(Comparator.comparing(Group::getGroupName));
        return groups;
    }

    private Group getOrCreateGroup(String groupCrn) {
        String[] splittedCrn = groupCrn.split(":");
        String accountId = splittedCrn[4];

        accountGroups.computeIfAbsent(accountId, this::createGroups);
        Map<String, Group> groups = accountGroups.get(accountId);

        groups.computeIfAbsent(groupCrn, this::createGroupFromCrn);
        return groups.get(groupCrn);
    }

    private Map<String, Group> createGroups(String accountId) {
        Map<String, Group> groups = new HashMap(GROUPS_PER_ACCOUNT);
        for (int i = 0; i < GROUPS_PER_ACCOUNT; ++i) {
            Group group = createGroup(accountId, "mockgroup" + i);
            groups.put(group.getCrn(), group);
        }
        return groups;
    }

    private Group createGroup(String accountId, String groupName) {
        String groupId = UUID.randomUUID().toString();
        String groupCrn = createCrn(accountId, Crn.Service.IAM, Crn.ResourceType.GROUP, groupId).toString();
        return Group.newBuilder()
                .setGroupId(groupId)
                .setCrn(groupCrn)
                .setGroupName(groupName)
                .build();
    }

    private Group createGroupFromCrn(String groupCrn) {
        String[] splittedCrn = groupCrn.split(":");
        String groupId = splittedCrn[6];
        return Group.newBuilder()
                .setGroupId(groupId)
                .setCrn(groupCrn)
                .setGroupName(groupId)
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
            String crnString;
            if (splittedCrn.length > 1) {
                userName = splittedCrn[6];
                accountId = splittedCrn[4];
                crnString = machineUserIdOrCrn;
            } else {
                userName = machineUserIdOrCrn;
                accountId = UUID.randomUUID().toString();
                Crn crn = createCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn(), Crn.ResourceType.MACHINE_USER, userName);
                accountId = crn.getAccountId();
                crnString = crn.toString();
            }
            responseObserver.onNext(
                    ListMachineUsersResponse.newBuilder()
                            .addMachineUser(MachineUser.newBuilder()
                                    .setMachineUserId(UUID.nameUUIDFromBytes((accountId + "#" + userName).getBytes()).toString())
                                    .setCrn(crnString)
                                    .setWorkloadUsername(sanitizeWorkloadUsername(userName))
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
        String accessKeyId = null;
        String privateKey = null;
        AltusCredential altusCredential = UserManagementProto.AccessKeyType.Value.UNSET.equals(request.getType())
                ? telemetyPublisherCredential : fluentCredential;
        if (altusCredential != null) {
            accessKeyId = altusCredential.getAccessKey();
            privateKey = new String(altusCredential.getPrivateKey());
        } else {
            accessKeyId = UUID.randomUUID().toString();
            privateKey = UUID.randomUUID().toString();
        }
        responseObserver.onNext(UserManagementProto.CreateAccessKeyResponse.newBuilder()
                .setPrivateKey(privateKey)
                .setAccessKey(UserManagementProto.AccessKey.newBuilder()
                        .setAccessKeyId(accessKeyId)
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

    @Override
    public void getIdPMetadataForWorkloadSSO(
            UserManagementProto.GetIdPMetadataForWorkloadSSORequest request,
            StreamObserver<UserManagementProto.GetIdPMetadataForWorkloadSSOResponse> responseObserver) {
        checkArgument(!Strings.isNullOrEmpty(request.getAccountId()));
        try {
            String metadata = Resources.toString(
                    Resources.getResource("sso/cdp-idp-metadata.xml"),
                    Charsets.UTF_8).trim();
            metadata = metadata.replace("accountId_REPLACE_ME", request.getAccountId());
            metadata = metadata.replace("hostname_REPLACE_ME", "localhost");
            responseObserver.onNext(
                    GetIdPMetadataForWorkloadSSOResponse.newBuilder()
                            .setMetadata(metadata)
                            .build());
            responseObserver.onCompleted();
        } catch (IOException e) {
            throw Status.INTERNAL
                    .withDescription("Could not find IdP metadata resource")
                    .withCause(e)
                    .asRuntimeException();
        }
    }

    @Override
    public void listRoles(UserManagementProto.ListRolesRequest request, StreamObserver<UserManagementProto.ListRolesResponse> responseObserver) {
        responseObserver.onNext(UserManagementProto.ListRolesResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest request,
                                            io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement
                                                .UserManagementProto.GetActorWorkloadCredentialsResponse> responseObserver) {

        final int keyType17 = 17;
        final int keyType18 = 18;
        final int saltType = 4;
        GetActorWorkloadCredentialsResponse.Builder respBuilder = GetActorWorkloadCredentialsResponse.getDefaultInstance().toBuilder();
        respBuilder.addKerberosKeysBuilder(0)
            .setSaltType(saltType)
            .setKeyType(keyType17)
            .setKeyValue("testKeyValue17")
            .setSaltValue("NonIodizedGrainOfSalt")
            .build();

        respBuilder.addKerberosKeysBuilder(1)
            .setSaltType(saltType)
            .setKeyType(keyType18)
            .setKeyValue("testKeyValue18")
            .setSaltValue("IodizedGrainOfSalt")
            .build();

        respBuilder.setPasswordHash("015353916be1489289e59166124bbcf21c78595f3717f71b079c469c513e05e7");
        responseObserver.onNext(respBuilder.build());
        responseObserver.onCompleted();
    }

    private String getLicense() {
        if (Files.exists(Paths.get(cmLicenseFilePath))) {
            try {
                String license = Files.readString(Path.of(cmLicenseFilePath));
                LOG.info("Cloudbreak license file successfully loaded.");
                return license;
            } catch (IOException e) {
                throw new RuntimeException("Error during reading license.", e);
            }
        } else {
            throw new IllegalStateException("The license file could not be found on path: '" + cmLicenseFilePath + "'. "
                    + "Please place your CM license file in your '<cbd_path>/etc' folder. "
                    + "By default the name of the file should be 'license.txt'.");
        }
    }

    private AltusCredential getAltusCredential(String altusCredentialFile, String altusCredentialProfile) {
        if (StringUtils.isNoneEmpty(altusCredentialFile, altusCredentialProfile)
                && Files.exists(Paths.get(altusCredentialFile))) {
            try {
                Map<String, Properties> propsMap = iniUtil.parseIni(new FileReader(altusCredentialFile));
                if (propsMap.containsKey(altusCredentialProfile)) {
                    Properties prop = propsMap.get(altusCredentialProfile);
                    String accessKey = prop.getProperty(ALTUS_ACCESS_KEY_ID, prop.getProperty(CDP_ACCESS_KEY_ID));
                    String privateKey = prop.getProperty(ALTUS_PRIVATE_KEY, prop.getProperty(CDP_PRIVATE_KEY));
                    return new AltusCredential(accessKey, privateKey.toCharArray());
                }
            } catch (IOException e) {
                LOG.warn("Error occurred during reading altus credential.", e);
            }
        }
        return null;
    }

    private UserManagementProto.ResourceAssignee createResourceAssignee(String resourceCrn) {
        return UserManagementProto.ResourceAssignee.newBuilder()
                .setAssigneeCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                .setResourceRoleCrn(createCrn(resourceCrn, Crn.ResourceType.RESOURCE_ROLE, "WorkspaceManager").toString())
                .build();
    }

    private UserManagementProto.ResourceAssignment createResourceAssigment(String assigneeCrn) {
        String resourceCrn = createCrn(assigneeCrn, Crn.ResourceType.WORKSPACE, Crn.fromString(assigneeCrn).getAccountId()).toString();
        return UserManagementProto.ResourceAssignment.newBuilder()
                .setResourceCrn(resourceCrn)
                .setResourceRoleCrn(createCrn(assigneeCrn, Crn.ResourceType.RESOURCE_ROLE, "WorkspaceManager").toString())
                .build();
    }

    private Crn createCrn(String baseCrn, Crn.ResourceType resourceType, String resource) {
        Crn crn = Crn.fromString(baseCrn);
        return createCrn(crn.getAccountId(), crn.getService(), resourceType, resource);
    }

    private Crn createCrn(String accountId, Crn.Service service, Crn.ResourceType resourceType, String resource) {
        return Crn.builder()
                .setAccountId(accountId)
                .setService(service)
                .setResourceType(resourceType)
                .setResource(resource)
                .build();
    }

    @VisibleForTesting
    String sanitizeWorkloadUsername(String userName) {
        return StringUtils.substringBefore(userName, "@").toLowerCase().replaceAll("[^a-z0-9_]", "");
    }
}