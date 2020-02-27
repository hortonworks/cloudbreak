package com.sequenceiq.caas.grpc.service;

import static com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.newSetFromMap;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.security.jwt.JwtHelper.decodeAndVerify;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse;
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
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadPasswordPolicy;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Resources;
import com.google.protobuf.util.JsonFormat;
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

    @VisibleForTesting
    static final long PASSWORD_LIFETIME = 31449600000L;

    private static final String ENV_ACCESS_RIGHT = "environments/accessEnvironment";

    private static final MacSigner SIGNATURE_VERIFIER = new MacSigner("titok");

    private static final Logger LOG = LoggerFactory.getLogger(MockUserManagementService.class);

    private static final int FIRST_GROUP = 0;

    private static final String ALTUS_ACCESS_KEY_ID = "altus_access_key_id";

    private static final String ALTUS_PRIVATE_KEY = "altus_private_key";

    private static final String CDP_ACCESS_KEY_ID = "cdp_access_key_id";

    private static final String CDP_PRIVATE_KEY = "cdp_private_key";

    private static final int MOCK_USER_COUNT = 10;

    private static final String ACCOUNT_SUBDOMAIN = "xcu2-8y8x";

    private static final String ACCOUNT_ID_ALTUS = "altus";

    private static final long CREATION_DATE_MS = 1483228800000L;

    private static final String ALL_RIGHTS_AND_RESOURCES = "*";

    private static final String REVERSE_SSH_TUNNEL = "CDP_REVERSE_SSH_TUNNEL";

    private static final String CDP_AZURE = "CDP_AZURE";

    private static final String CDP_AUTOMATIC_USERSYNC_POLLER = "CDP_AUTOMATIC_USERSYNC_POLLER";

    private static final String CLOUDERA_INTERNAL_ACCOUNT = "CLOUDERA_INTERNAL_ACCOUNT";

    private static final String CDP_BASE_IMAGE = "CDP_BASE_IMAGE";

    private static final String LOCAL_DEV = "LOCAL_DEV";

    @Inject
    private JsonUtil jsonUtil;

    @Inject
    private IniUtil iniUtil;

    @Inject
    private MockCrnService mockCrnService;

    @Inject
    private MockGroupManagementService mockGroupManagementService;

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

    @Value("${auth.mock.ccm.enable}")
    private boolean enableCcm;

    @Value("${auth.mock.baseimage.enable}")
    private boolean enableBaseImages;

    private String cbLicense;

    private AltusCredential telemetyPublisherCredential;

    private AltusCredential fluentCredential;

    private final Map<String, Set<String>> accountUsers = new ConcurrentHashMap<>();

    @Value("${auth.mock.event-generation.expiration-minutes:10}")
    private long eventGenerationExirationMinutes;

    private LoadingCache<String, GetEventGenerationIdsResponse> eventGenerationIdsCache;

    private GetActorWorkloadCredentialsResponse actorWorkloadCredentialsResponse;

    private UserManagementProto.WorkloadPasswordPolicy workloadPasswordPolicy;

    @PostConstruct
    public void init() {
        this.cbLicense = getLicense();
        this.telemetyPublisherCredential = getAltusCredential(databusTpCredentialFile, databusTpCredentialProfile);
        this.fluentCredential = getAltusCredential(databusFluentCredentialFile, databusFluentCredentialProfile);
        this.eventGenerationIdsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(eventGenerationExirationMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<String, GetEventGenerationIdsResponse>() {
                    @Override
                    public GetEventGenerationIdsResponse load(String key) throws Exception {
                        GetEventGenerationIdsResponse.Builder respBuilder = GetEventGenerationIdsResponse.newBuilder();
                        respBuilder.setLastRoleAssignmentEventId(UUID.randomUUID().toString());
                        respBuilder.setLastResourceRoleAssignmentEventId(UUID.randomUUID().toString());
                        respBuilder.setLastGroupMembershipChangedEventId(UUID.randomUUID().toString());
                        respBuilder.setLastActorDeletedEventId(UUID.randomUUID().toString());
                        respBuilder.setLastActorWorkloadCredentialsChangedEventId(UUID.randomUUID().toString());
                        return respBuilder.build();
                    }
                });

        initializeActorWorkloadCredentials();
        initializeWorkloadPasswordPolicy();
    }

    @VisibleForTesting
    void initializeActorWorkloadCredentials() {

        GetActorWorkloadCredentialsResponse.Builder builder = GetActorWorkloadCredentialsResponse.newBuilder();
        try {
            JsonFormat.parser().merge(Resources.toString(
                    Resources.getResource("mock-responses/ums/get-workload-credentials.json"), StandardCharsets.UTF_8), builder);
            actorWorkloadCredentialsResponse = builder.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    void initializeWorkloadPasswordPolicy() {
        WorkloadPasswordPolicy.Builder builder = WorkloadPasswordPolicy.newBuilder();
        builder.setWorkloadPasswordMaxLifetime(PASSWORD_LIFETIME);
        workloadPasswordPolicy = builder.build();
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
        List<Group> groups = List.copyOf(mockGroupManagementService.getOrCreateGroups(accountId));
        PolicyStatement policyStatement = PolicyStatement.newBuilder()
                .addRight(ALL_RIGHTS_AND_RESOURCES)
                .addResource(ALL_RIGHTS_AND_RESOURCES)
                .build();
        PolicyDefinition policyDefinition = PolicyDefinition.newBuilder().addStatement(policyStatement).build();
        Policy powerUserPolicy = Policy.newBuilder()
                .setCrn(mockCrnService.createCrn(ACCOUNT_ID_ALTUS, Crn.Service.IAM, Crn.ResourceType.POLICY, "PowerUserPolicy").toString())
                .setCreationDateMs(CREATION_DATE_MS)
                .setPolicyDefinition(policyDefinition)
                .build();
        Role powerUserRole = Role.newBuilder()
                .setCrn(mockCrnService.createCrn(ACCOUNT_ID_ALTUS, Crn.Service.IAM, Crn.ResourceType.ROLE, "PowerUser").toString())
                .setCreationDateMs(CREATION_DATE_MS)
                .addPolicy(powerUserPolicy)
                .build();
        RoleAssignment roleAssignment = RoleAssignment.newBuilder().setRole(powerUserRole).build();
        GetRightsResponse.Builder responseBuilder = GetRightsResponse.newBuilder()
                .addRoleAssignment(roleAssignment)
                .addWorkloadAdministrationGroupName(mockGroupManagementService.generateVirtualGroupName(ENV_ACCESS_RIGHT));
        groups.stream().forEach(group -> responseBuilder.addGroupCrn(group.getCrn()));
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private User createUser(String accountId, String userName) {
        Crn actorCrn = Crn.safeFromString(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn());
        String userCrn = Crn.builder()
                .setService(actorCrn.getService())
                .setAccountId(accountId)
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
        mockGroupManagementService.listGroups(request, responseObserver);
    }

    @Override
    public void listMachineUsers(UserManagementProto.ListMachineUsersRequest request, StreamObserver<ListMachineUsersResponse> responseObserver) {
        if (request.getMachineUserNameOrCrnCount() == 0) {
            responseObserver.onNext(ListMachineUsersResponse.newBuilder().build());
        } else {
            String machineUserIdOrCrn = request.getMachineUserNameOrCrn(0);
            String[] splittedCrn = machineUserIdOrCrn.split(":");
            String userName;
            String accountId = request.getAccountId();
            String crnString;
            if (splittedCrn.length > 1) {
                userName = splittedCrn[6];
                crnString = machineUserIdOrCrn;
            } else {
                userName = machineUserIdOrCrn;
                Crn crn = mockCrnService.createCrn(accountId, Crn.Service.IAM, Crn.ResourceType.MACHINE_USER, userName);
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
        UserManagementProto.Account.Builder builder = UserManagementProto.Account.newBuilder();
        if (enableCcm) {
            builder.addEntitlements(createEntitlement(REVERSE_SSH_TUNNEL));
        }
        if (enableBaseImages) {
            builder.addEntitlements(createEntitlement(CDP_BASE_IMAGE));
        }
        responseObserver.onNext(
                GetAccountResponse.newBuilder()
                        .setAccount(builder
                                .setClouderaManagerLicenseKey(cbLicense)
                                .setWorkloadSubdomain(ACCOUNT_SUBDOMAIN)
                                .addEntitlements(createEntitlement(CDP_AZURE))
                                .addEntitlements(createEntitlement(CDP_AUTOMATIC_USERSYNC_POLLER))
                                .addEntitlements(createEntitlement(CLOUDERA_INTERNAL_ACCOUNT))
                                .addEntitlements(createEntitlement(LOCAL_DEV))
                                .setPasswordPolicy(workloadPasswordPolicy)
                                .build())
                        .build());
        responseObserver.onCompleted();
    }

    private UserManagementProto.Entitlement createEntitlement(String entitlementName) {
        return UserManagementProto.Entitlement.newBuilder()
                .setEntitlementName(entitlementName)
                .build();
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

    /**
     * NOTE: This is a mock implementation and meant only for testing.
     * This implementation returns hard coded pre-defined set of response for workload credentials.
     * For any integration test of debugging purpose, this should not be used and intent is purely mock.
     * Hashed value used internally is sha256 hash of <i>Password123!</i>.
     */
    @Override
    public void getActorWorkloadCredentials(com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest request,
            io.grpc.stub.StreamObserver<com.cloudera.thunderhead.service.usermanagement
                    .UserManagementProto.GetActorWorkloadCredentialsResponse> responseObserver) {

        GetActorWorkloadCredentialsResponse.Builder builder = GetActorWorkloadCredentialsResponse.newBuilder(actorWorkloadCredentialsResponse);
        builder.setPasswordHashExpirationDate(System.currentTimeMillis() + PASSWORD_LIFETIME);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getEventGenerationIds(GetEventGenerationIdsRequest request, StreamObserver<GetEventGenerationIdsResponse> responseObserver) {
        mockCrnService.ensureInternalActor();
        try {
            responseObserver.onNext(eventGenerationIdsCache.get(request.getAccountId()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            throw Status.INTERNAL
                    .withDescription("Error retrieving/creating event generation ids.")
                    .withCause(e)
                    .asRuntimeException();
        }
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

    @Override
    public void getWorkloadAdministrationGroupName(UserManagementProto.GetWorkloadAdministrationGroupNameRequest request,
            StreamObserver<UserManagementProto.GetWorkloadAdministrationGroupNameResponse> responseObserver) {
        mockGroupManagementService.getWorkloadAdministrationGroupName(request, responseObserver);
    }

    @Override
    public void setWorkloadAdministrationGroupName(UserManagementProto.SetWorkloadAdministrationGroupNameRequest request,
            StreamObserver<UserManagementProto.SetWorkloadAdministrationGroupNameResponse> responseObserver) {
        mockGroupManagementService.setWorkloadAdministrationGroupName(request, responseObserver);
    }

    @Override
    public void deleteWorkloadAdministrationGroupName(UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest request,
            StreamObserver<UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse> responseObserver) {
        mockGroupManagementService.deleteWorkloadAdministrationGroupName(request, responseObserver);
    }

    private UserManagementProto.ResourceAssignee createResourceAssignee(String resourceCrn) {
        return UserManagementProto.ResourceAssignee.newBuilder()
                .setAssigneeCrn(GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn())
                .setResourceRoleCrn(mockCrnService.createCrn(resourceCrn, Crn.ResourceType.RESOURCE_ROLE, "WorkspaceManager").toString())
                .build();
    }

    private UserManagementProto.ResourceAssignment createResourceAssigment(String assigneeCrn) {
        String resourceCrn = mockCrnService.createCrn(assigneeCrn, Crn.ResourceType.WORKSPACE, Crn.fromString(assigneeCrn).getAccountId()).toString();
        return UserManagementProto.ResourceAssignment.newBuilder()
                .setResourceCrn(resourceCrn)
                .setResourceRoleCrn(mockCrnService.createCrn(assigneeCrn, Crn.ResourceType.RESOURCE_ROLE, "WorkspaceManager").toString())
                .build();
    }

    @VisibleForTesting
    String sanitizeWorkloadUsername(String userName) {
        return StringUtils.substringBefore(userName, "@").toLowerCase().replaceAll("[^a-z0-9_]", "");
    }
}