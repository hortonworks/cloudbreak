package com.sequenceiq.freeipa.service.freeipa.user;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.model.Conversions;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UmsUsersStateProviderTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ACTOR_CRN = Crn.builder()
            .setAccountId(ACCOUNT_ID)
            .setPartition(Crn.Partition.CDP)
            .setResourceType(Crn.ResourceType.USER)
            .setService(Crn.Service.IAM)
            .setResource(UUID.randomUUID().toString())
            .build()
            .toString();

    private static final String ENVIRONMENT_CRN = createEnvironmentCrn();

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private UmsCredentialProvider umsCredentialProvider;

    @Mock
    private UmsRightProvider umsRightProvider;

    @InjectMocks
    private UmsUsersStateProvider underTest;

    private UserSyncTestData testData = createUserSyncTestData();

    @Test
    void getEnvToUmsUsersStateMap() {
        setupMocks();

        Map<String, UmsUsersState.Builder> umsUsersStateBuilderMap = underTest.getUmsUsersStateMap(
                ACCOUNT_ID, ACTOR_CRN, List.of(ENVIRONMENT_CRN), Set.of(), Set.of(), Optional.empty(), true);

        verifyUmsUsersStateBuilderMap(umsUsersStateBuilderMap);
    }

    @Test
    void getUmsUsersStateMapBulk() {
        setupMocksForBulk();

        Map<String, UmsUsersState.Builder> umsUsersStateBuilderMap = underTest.getUmsUsersStateMapBulk(
                ACCOUNT_ID, List.of(ENVIRONMENT_CRN), Optional.empty());

        verifyUmsUsersStateBuilderMap(umsUsersStateBuilderMap);
    }

    private UserSyncTestData createUserSyncTestData() {
        return new UserSyncTestData();
    }

    private void setupMocks() {
        doAnswer(invocation -> invocation.getArgument(0, AuthorizationResourceAction.class).getRight())
                .when(umsRightProvider).getRight(
                        any(AuthorizationResourceAction.class), eq(INTERNAL_ACTOR_CRN), eq(ACCOUNT_ID));

        when(grpcUmsClient.listAllGroups(eq(INTERNAL_ACTOR_CRN), eq(ACCOUNT_ID), any(Optional.class)))
                .thenReturn(testData.groups);
        when(grpcUmsClient.listWorkloadAdministrationGroups(eq(INTERNAL_ACTOR_CRN),
                eq(ACCOUNT_ID), any(Optional.class)))
                .thenReturn(testData.allWags);


        when(grpcUmsClient.listAllUsers(eq(ACTOR_CRN), eq(ACCOUNT_ID), any(Optional.class)))
                .thenReturn(testData.users);

        when(grpcUmsClient.listAllMachineUsers(eq(ACTOR_CRN), eq(ACCOUNT_ID),
                eq(UmsUsersStateProvider.INCLUDE_INTERNAL_MACHINE_USERS), any(Optional.class)))
                .thenReturn(testData.machineUsers);

        doAnswer(invocation -> {
            String crn = invocation.getArgument(1, String.class);
            Map<String, Boolean> actorRights = testData.memberCrnToActorRights.get(crn);
            return UmsUsersStateProvider.RIGHTS.stream()
                    .map(right -> actorRights.get(right))
                    .collect(Collectors.toList());
        }).when(grpcUmsClient).hasRights(eq(INTERNAL_ACTOR_CRN), anyString(), any(List.class), any(Optional.class));

        doAnswer(invocation -> {
            String memberCrn = invocation.getArgument(2, String.class);
            return testData.memberCrnToGroupMembership.get(memberCrn).entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }).when(grpcUmsClient)
                .listGroupsForMember(eq(INTERNAL_ACTOR_CRN), eq(ACCOUNT_ID), anyString(), any(Optional.class));

        doAnswer(invocation -> {
            String memberCrn = invocation.getArgument(1, String.class);
            return testData.memberCrnToWagMembership.get(memberCrn).entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }).when(grpcUmsClient)
                .listWorkloadAdministrationGroupsForMember(eq(INTERNAL_ACTOR_CRN), anyString(), any(Optional.class));

        doAnswer(invocation -> Conversions
                .toWorkloadCredential(
                        testData.memberCrnToWorkloadCredentials.get(invocation.getArgument(0, String.class))))
                .when(umsCredentialProvider)
                .getCredentials(anyString(), any(Optional.class));

        setupServicePrincipals();
    }

    private void setupMocksForBulk() {
        List<Pair<String, List<String>>> expectedRightsChecks =
                List.of(Pair.of(ENVIRONMENT_CRN, UmsUsersStateProvider.RIGHTS));

        UserManagementProto.GetUserSyncStateModelResponse.Builder builder =
                UserManagementProto.GetUserSyncStateModelResponse.newBuilder();

        builder.addAllGroup(testData.groups);
        builder.addAllWorkloadAdministrationGroup(testData.allWags);
        builder.addAllActor(Stream.concat(
                testData.users.stream()
                        .map(u ->  {
                            Map<String, Boolean> groupMembership = testData.memberCrnToGroupMembership.get(u.getCrn());
                            Map<String, Boolean> wagMembership = testData.memberCrnToWagMembership.get(u.getCrn());
                            return UserManagementProto.UserSyncActor.newBuilder()
                                    .setActorDetails(UserManagementProto.UserSyncActorDetails.newBuilder()
                                            .setCrn(u.getCrn())
                                            .setWorkloadUsername(u.getWorkloadUsername())
                                            .setFirstName(u.getFirstName())
                                            .setLastName(u.getLastName())
                                            .addAllCloudIdentity(u.getCloudIdentitiesList())
                                            .build())
                                    .addRightsCheckResult(UserManagementProto.RightsCheckResult.newBuilder()
                                            .addAllHasRight(UmsUsersStateProvider.RIGHTS.stream()
                                                    .map(right -> testData.memberCrnToActorRights.get(u.getCrn()).get(right))
                                                    .collect(Collectors.toList()))
                                            .build())
                                    .addAllGroupIndex(IntStream.range(0, testData.groups.size())
                                            .filter(i -> groupMembership.get(testData.groups.get(i).getCrn()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .addAllWorkloadAdministrationGroupIndex(IntStream.range(0, testData.allWags.size())
                                            .filter(i -> wagMembership
                                                    .get(testData.allWags.get(i).getWorkloadAdministrationGroupName()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .setCredentials(toActorWorkloadCredentials(
                                            testData.memberCrnToWorkloadCredentials.get(u.getCrn())))
                                    .build();
                        }),
                testData.machineUsers.stream()
                        .map(u -> {
                            Map<String, Boolean> groupMembership = testData.memberCrnToGroupMembership.get(u.getCrn());
                            Map<String, Boolean> wagMembership = testData.memberCrnToWagMembership.get(u.getCrn());
                            return UserManagementProto.UserSyncActor.newBuilder()
                                    .setActorDetails(UserManagementProto.UserSyncActorDetails.newBuilder()
                                            .setCrn(u.getCrn())
                                            .setWorkloadUsername(u.getWorkloadUsername())
                                            .setFirstName(u.getMachineUserName())
                                            .setLastName(u.getMachineUserId())
                                            .addAllCloudIdentity(u.getCloudIdentitiesList())
                                            .build())
                                    .addRightsCheckResult(UserManagementProto.RightsCheckResult.newBuilder()
                                            .addAllHasRight(UmsUsersStateProvider.RIGHTS.stream()
                                                    .map(right -> testData.memberCrnToActorRights.get(u.getCrn()).get(right))
                                                    .collect(Collectors.toList()))
                                            .build())
                                    .addAllGroupIndex(IntStream.range(0, testData.groups.size())
                                            .filter(i -> groupMembership.get(testData.groups.get(i).getCrn()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .addAllWorkloadAdministrationGroupIndex(IntStream.range(0, testData.allWags.size())
                                            .filter(i -> wagMembership
                                                    .get(testData.allWags.get(i).getWorkloadAdministrationGroupName()))
                                            .boxed()
                                            .collect(Collectors.toList()))
                                    .setCredentials(toActorWorkloadCredentials(
                                            testData.memberCrnToWorkloadCredentials.get(u.getCrn())))
                                    .build();
                        }))
                .collect(Collectors.toList()));

        when(grpcUmsClient.getUserSyncStateModel(
                eq(INTERNAL_ACTOR_CRN), eq(ACCOUNT_ID), eq(expectedRightsChecks), any(Optional.class)))
                .thenReturn(builder.build());
        setupServicePrincipals();
    }

    private void setupServicePrincipals() {
        when(grpcUmsClient.listServicePrincipalCloudIdentities(
                eq(INTERNAL_ACTOR_CRN), eq(ACCOUNT_ID), eq(ENVIRONMENT_CRN), any(Optional.class)))
                .thenReturn(testData.servicePrincipalCloudIdentities);
    }

    private void verifyUmsUsersStateBuilderMap(Map<String, UmsUsersState.Builder> umsUsersStateBuildersMap) {
        assertEquals(1, umsUsersStateBuildersMap.size());
        UmsUsersState.Builder builder = umsUsersStateBuildersMap.get(ENVIRONMENT_CRN);
        assertNotNull(builder);
        UmsUsersState state = builder.build();

        // Add the internal group to the expected groups and wags
        assertEquals(testData.groups.size() + testData.wagsForThisEnvironment.size() + 1,
                state.getUsersState().getGroups().size());
        List<String> groupNames = state.getUsersState().getGroups()
                .stream().map(FmsGroup::getName).collect(Collectors.toList());
        assertTrue(groupNames.containsAll(
                testData.groups.stream().map(UserManagementProto.Group::getGroupName).collect(Collectors.toList())));
        assertTrue(groupNames.containsAll(
                testData.wagsForThisEnvironment.stream()
                        .map(UserManagementProto.WorkloadAdministrationGroup::getWorkloadAdministrationGroupName)
                        .collect(Collectors.toList())));
        assertEquals(
                testData.allWags.stream()
                        .map(UserManagementProto.WorkloadAdministrationGroup::getWorkloadAdministrationGroupName)
                        .collect(Collectors.toSet()),
                state.getWorkloadAdministrationGroups().stream()
                        .map(FmsGroup::getName)
                        .collect(Collectors.toSet()));

        assertEquals(Stream.concat(testData.users.stream().map(UserManagementProto.User::getWorkloadUsername),
                testData.machineUsers.stream()
                        .map(UserManagementProto.MachineUser::getWorkloadUsername))
                        .collect(Collectors.toSet()),
                state.getRequestedWorkloadUsernames());

        // users including rights, group membership, wags
        // machine users including rights, group membership, wags
        UsersState usersState = state.getUsersState();
        Set<String> workloadUsersWithAccess = usersState.getUsers().stream()
                .map(FmsUser::getName)
                .collect(Collectors.toSet());
        Multimap<String, String> groupsPerMember = Multimaps.invertFrom(usersState.getGroupMembership(),
                ArrayListMultimap.<String, String>create());
        testData.users.forEach(u ->
                verifyActor(u.getCrn(), u.getWorkloadUsername(), usersState, workloadUsersWithAccess,
                        groupsPerMember.get(u.getWorkloadUsername()),
                        state.getUsersWorkloadCredentialMap().get(u.getWorkloadUsername())));
        testData.machineUsers.forEach(u ->
                verifyActor(u.getCrn(), u.getWorkloadUsername(), usersState, workloadUsersWithAccess,
                        groupsPerMember.get(u.getWorkloadUsername()),
                        state.getUsersWorkloadCredentialMap().get(u.getWorkloadUsername())));

        assertEquals(testData.servicePrincipalCloudIdentities, state.getServicePrincipalCloudIdentities());
    }

    private void verifyActor(
            String actorCrn, String workloadUsername,
            UsersState usersState, Set<String> workloadUsersWithAccess,
            Collection<String> actualGroups,
            WorkloadCredential workloadCredential) {
        if (testData.memberCrnToActorRights.get(actorCrn).get(UmsUsersStateProvider.RIGHTS.get(0))) {
            assertTrue(workloadUsersWithAccess.contains(workloadUsername));
            Map<String, Boolean> expectedGroupMembership = testData.memberCrnToGroupMembership.get(actorCrn);
            testData.groups.forEach(g -> {
                if (expectedGroupMembership.get(g.getCrn())) {
                    assertTrue(actualGroups.contains(g.getGroupName()));
                } else {
                    assertFalse(actualGroups.contains(g.getGroupName()));
                }
            });
            Map<String, Boolean> expectedWagMembership = testData.memberCrnToWagMembership.get(actorCrn);
            testData.wagsForThisEnvironment.forEach(wag -> {
                if (expectedWagMembership.get(wag.getWorkloadAdministrationGroupName())) {
                    assertTrue(actualGroups.contains(wag.getWorkloadAdministrationGroupName()));
                } else {
                    assertFalse(actualGroups.contains(wag.getWorkloadAdministrationGroupName()));
                }
            });
            testData.wagsForOtherEnvironment.forEach(wag ->
                    assertFalse(actualGroups.contains(wag.getWorkloadAdministrationGroupName())));
            verifyWorkloadCredential(actorCrn, workloadCredential);
        } else {
            assertFalse(workloadUsersWithAccess.contains(workloadUsername));
            assertNull(workloadCredential);
        }
    }

    private void verifyWorkloadCredential(String actorCrn, WorkloadCredential workloadCredential) {
        assertNotNull(workloadCredential);
        UserManagementProto.GetActorWorkloadCredentialsResponse expected =
                testData.memberCrnToWorkloadCredentials.get(actorCrn);
        assertEquals(expected.getPasswordHash(), workloadCredential.getHashedPassword());
        assertEquals(expected.getKerberosKeysList(), workloadCredential.getKeys());
        assertEquals(expected.getSshPublicKeyList(), workloadCredential.getSshPublicKeys());
    }

    private static UserManagementProto.ActorWorkloadCredentials toActorWorkloadCredentials(
            UserManagementProto.GetActorWorkloadCredentialsResponse response) {
        return UserManagementProto.ActorWorkloadCredentials.newBuilder()
                .setPasswordHash(response.getPasswordHash())
                .setPasswordHashExpirationDate(response.getPasswordHashExpirationDate())
                .addAllKerberosKeys(response.getKerberosKeysList())
                .addAllSshPublicKey(response.getSshPublicKeyList())
                .build();
    }

    private static String createEnvironmentCrn() {
        return Crn.builder()
                .setAccountId(ACCOUNT_ID)
                .setPartition(Crn.Partition.CDP)
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setService(Crn.Service.ENVIRONMENTS)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    private static class UserSyncTestData {
        // groups
        List<UserManagementProto.Group> groups = createGroups("testGroup", 10);

        // wags
        List<UserManagementProto.WorkloadAdministrationGroup> wagsForThisEnvironment =
                createWags(ENVIRONMENT_CRN, 10);

        List<UserManagementProto.WorkloadAdministrationGroup> wagsForOtherEnvironment =
                createWags(createEnvironmentCrn(), 10);

        List<UserManagementProto.WorkloadAdministrationGroup> allWags = Stream.concat(
                wagsForThisEnvironment.stream(),
                wagsForOtherEnvironment.stream())
                .collect(Collectors.toList());

        // users and machine users
        List<UserManagementProto.User> users = createUsers("testUser", 10);

        List<UserManagementProto.MachineUser> machineUsers = createMachineUsers(10);

        List<String> allActorCrns = Stream.concat(users.stream().map(UserManagementProto.User::getCrn),
                machineUsers.stream().map(UserManagementProto.MachineUser::getCrn))
                .collect(Collectors.toList());

        // mappings from actor to rights, group memberships, wag memberships, and credentials
        Map<String, Map<String, Boolean>> memberCrnToActorRights = createActorRights(allActorCrns);

        Map<String, Map<String, Boolean>> memberCrnToGroupMembership = createGroupMembership(allActorCrns);

        Map<String, Map<String, Boolean>> memberCrnToWagMembership = createWagMembership(allActorCrns);

        // credentials
        Map<String, UserManagementProto.GetActorWorkloadCredentialsResponse> memberCrnToWorkloadCredentials =
                createCredentials(allActorCrns);

        // service principals cloud identities
        List<UserManagementProto.ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities =
                createServicePrincipalCloudIdentities(5);

        private Map<String, UserManagementProto.GetActorWorkloadCredentialsResponse> createCredentials(
                List<String> allActorCrns) {
            Map<String, String> actorCrnToWorkloadUsername = Maps.newHashMap();
            actorCrnToWorkloadUsername.putAll(users.stream()
                    .collect(Collectors.toMap(UserManagementProto.User::getCrn,
                            UserManagementProto.User::getWorkloadUsername)));
            actorCrnToWorkloadUsername.putAll(machineUsers.stream()
                    .collect(Collectors.toMap(UserManagementProto.MachineUser::getCrn,
                            UserManagementProto.MachineUser::getWorkloadUsername)));
            return allActorCrns.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            actorCrn -> createActorCredentials(actorCrnToWorkloadUsername.get(actorCrn))));
        }

        private UserManagementProto.GetActorWorkloadCredentialsResponse createActorCredentials(
                String workloadUsername) {
            return UserManagementProto.GetActorWorkloadCredentialsResponse.newBuilder()
                    .setWorkloadUsername(workloadUsername)
                    .setPasswordHash(RandomStringUtils.randomAlphabetic(50))
                    .setPasswordHashExpirationDate(RANDOM.nextLong())
                    .addKerberosKeys(UserManagementProto.ActorKerberosKey.newBuilder()
                            .setKeyType(RANDOM.nextInt())
                            .setKeyValue(RandomStringUtils.randomAlphabetic(10))
                            .setSaltType(RANDOM.nextInt())
                            .setSaltValue(RandomStringUtils.randomAlphabetic(10))
                            .build())
                    .addSshPublicKey(UserManagementProto.SshPublicKey.newBuilder()
                            .setPublicKey(RandomStringUtils.randomAlphabetic(50))
                            .setPublicKeyFingerprint(RandomStringUtils.randomAlphabetic(10))
                            .setDescription(RandomStringUtils.randomAlphabetic(10))
                            .build())
                    .build();
        }

        private Map<String, Map<String, Boolean>> createActorRights(List<String> allActorCrns) {
            return allActorCrns.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            actor -> createRandomBooleans(UmsUsersStateProvider.RIGHTS)));
        }

        private Map<String, Map<String, Boolean>> createGroupMembership(List<String> allActorCrns) {
            List<String> groupCrns = groups.stream()
                    .map(UserManagementProto.Group::getCrn)
                    .collect(Collectors.toList());
            return allActorCrns.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            actor -> createRandomBooleans(groupCrns)));
        }

        private Map<String, Map<String, Boolean>> createWagMembership(List<String> allActorCrns) {
            List<String> wagNames = allWags.stream()
                    .map(UserManagementProto.WorkloadAdministrationGroup::getWorkloadAdministrationGroupName)
                    .collect(Collectors.toList());
            return allActorCrns.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            actor -> createRandomBooleans(wagNames)));
        }

        private <U> Map<U, Boolean> createRandomBooleans(List<U> keys) {
            return keys.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            right -> RANDOM.nextBoolean()));
        }

        private List<UserManagementProto.User> createUsers(String userNameBasis, int numUsers) {
            return IntStream.range(0, numUsers)
                    .mapToObj(i -> {
                        return UserManagementProto.User.newBuilder()
                                .setFirstName(RandomStringUtils.randomAlphabetic(10))
                                .setLastName(RandomStringUtils.randomAlphabetic(10))
                                .setCrn(Crn.builder()
                                        .setAccountId(ACCOUNT_ID)
                                        .setPartition(Crn.Partition.CDP)
                                        .setResourceType(Crn.ResourceType.USER)
                                        .setService(Crn.Service.IAM)
                                        .setResource(UUID.randomUUID().toString())
                                        .build()
                                        .toString())
                                .setWorkloadUsername(userNameBasis + i)
                                .addCloudIdentities(createCloudIdentity())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        private List<UserManagementProto.MachineUser> createMachineUsers(int numUsers) {
            return IntStream.range(0, numUsers)
                    .mapToObj(i -> {
                        String id = UUID.randomUUID().toString();
                        return UserManagementProto.MachineUser.newBuilder()
                                .setMachineUserId(id)
                                .setMachineUserName(RandomStringUtils.randomAlphabetic(10))
                                .setCrn(Crn.builder()
                                        .setAccountId(ACCOUNT_ID)
                                        .setPartition(Crn.Partition.CDP)
                                        .setResourceType(Crn.ResourceType.MACHINE_USER)
                                        .setService(Crn.Service.IAM)
                                        .setResource(UUID.randomUUID().toString())
                                        .build()
                                        .toString())
                                .setWorkloadUsername(RandomStringUtils.randomAlphabetic(10))
                                .addCloudIdentities(createCloudIdentity())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        private List<UserManagementProto.Group> createGroups(String groupNameBasis, int numGroups) {
            return IntStream.range(0, numGroups)
                    .mapToObj(i -> {
                        String name = groupNameBasis + i;
                        String id = UUID.randomUUID().toString();
                        return UserManagementProto.Group.newBuilder()
                                .setGroupName(name)
                                .setCrn(Crn.builder()
                                        .setAccountId(ACCOUNT_ID)
                                        .setPartition(Crn.Partition.CDP)
                                        .setResourceType(Crn.ResourceType.GROUP)
                                        .setService(Crn.Service.IAM)
                                        .setResource(name + "/" + id)
                                        .build()
                                        .toString())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        private List<UserManagementProto.WorkloadAdministrationGroup> createWags(String environmentCrn, int numWags) {
            return IntStream.range(0, numWags)
                    .mapToObj(i -> UserManagementProto.WorkloadAdministrationGroup.newBuilder()
                            .setResource(environmentCrn)
                            .setRightName(UUID.randomUUID().toString())
                            .setWorkloadAdministrationGroupName(UUID.randomUUID().toString())
                            .build())
                    .collect(Collectors.toList());
        }

        private List<UserManagementProto.ServicePrincipalCloudIdentities> createServicePrincipalCloudIdentities(int numSP) {
            return IntStream.range(0, numSP)
                    .mapToObj(i -> UserManagementProto.ServicePrincipalCloudIdentities.newBuilder()
                            .setServicePrincipal(UUID.randomUUID().toString())
                            .addCloudIdentities(createCloudIdentity())
                            .build())
                    .collect(Collectors.toList());
        }

        private UserManagementProto.CloudIdentity createCloudIdentity() {
            return UserManagementProto.CloudIdentity.newBuilder()
                    .setCloudIdentityName(UserManagementProto.CloudIdentityName.newBuilder()
                            .setAzureCloudIdentityName(UserManagementProto.AzureCloudIdentityName.newBuilder()
                                    .setObjectId(UUID.randomUUID().toString())
                                    .build())
                            .build())
                    .setCloudIdentityDomain(UserManagementProto.CloudIdentityDomain.newBuilder()
                            .setEnvironmentCrn(ENVIRONMENT_CRN)
                            .setAzureCloudIdentityDomain(UserManagementProto.AzureCloudIdentityDomain.newBuilder()
                                    .setAzureAdIdentifier(UUID.randomUUID().toString())
                                    .build())
                            .build())
                    .build();
        }
    }
}