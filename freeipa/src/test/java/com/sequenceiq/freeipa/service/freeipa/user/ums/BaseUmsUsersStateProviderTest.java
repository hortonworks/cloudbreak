package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@ExtendWith(MockitoExtension.class)
class BaseUmsUsersStateProviderTest {

    protected static final SecureRandom RANDOM = new SecureRandom();

    protected static final String ACCOUNT_ID = UUID.randomUUID().toString();

    protected static final String ENVIRONMENT_CRN = createEnvironmentCrn();

    @Mock
    protected GrpcUmsClient grpcUmsClient;

    protected final UserSyncTestData testData = new UserSyncTestData();

    protected void setupServicePrincipals() {
        when(grpcUmsClient.listServicePrincipalCloudIdentities(
                eq(ACCOUNT_ID), eq(ENVIRONMENT_CRN)))
                .thenReturn(testData.servicePrincipalCloudIdentities);
    }

    protected void verifyUmsUsersStateBuilderMap(Map<String, UmsUsersState> umsUsersStateMap, UserSyncOptions options) {
        assertEquals(1, umsUsersStateMap.size());
        UmsUsersState state = umsUsersStateMap.get(ENVIRONMENT_CRN);
        assertNotNull(state);

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
                verifyActor(u.getCrn(), u.getWorkloadUsername(), workloadUsersWithAccess,
                        groupsPerMember.get(u.getWorkloadUsername()),
                        state.getUsersWorkloadCredentialMap().get(u.getWorkloadUsername()),
                        usersState.getUserMetadataMap().get(u.getWorkloadUsername())));
        testData.machineUsers.forEach(mu ->
                verifyActor(mu.getCrn(), mu.getWorkloadUsername(), workloadUsersWithAccess,
                        groupsPerMember.get(mu.getWorkloadUsername()),
                        state.getUsersWorkloadCredentialMap().get(mu.getWorkloadUsername()),
                        usersState.getUserMetadataMap().get(mu.getWorkloadUsername())));

        verifyLargeGroups(state, options);

        assertEquals(testData.servicePrincipalCloudIdentities, state.getServicePrincipalCloudIdentities());
    }

    private void verifyLargeGroups(UmsUsersState state, UserSyncOptions options) {
        Map<String, String> groupCrnToName = testData.groups.stream()
                .collect(Collectors.toMap(UserManagementProto.Group::getCrn, UserManagementProto.Group::getGroupName));
        Map<String, Integer> groupMembershipCount = new HashMap<>();
        Set<String> wagNamesForThisEnvironment = testData.wagsForThisEnvironment.stream()
                .map(UserManagementProto.WorkloadAdministrationGroup::getWorkloadAdministrationGroupName)
                .collect(Collectors.toSet());
        testData.memberCrnToActorRights.entrySet().stream()
                .filter(entry -> entry.getValue().get(UserSyncConstants.RIGHTS.get(0)))
                .map(Map.Entry::getKey)
                .forEach(actor -> {
                    testData.memberCrnToGroupMembership.get(actor).forEach((groupCrn, member) -> {
                        if (member) {
                            String groupName = groupCrnToName.get(groupCrn);
                            groupMembershipCount.put(groupName, groupMembershipCount.getOrDefault(groupName, 0) + 1);
                        }
                    });
                    testData.memberCrnToWagMembership.get(actor).forEach((wagName, member) -> {
                        if (member && wagNamesForThisEnvironment.contains(wagName)) {
                            groupMembershipCount.put(wagName, groupMembershipCount.getOrDefault(wagName, 0) + 1);
                        }
                    });
                });
        Set<String> expectedGroupsExceedingThreshold = groupMembershipCount.entrySet().stream()
                .filter(entry -> entry.getValue() > options.getLargeGroupThreshold())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<String> expectedGroupsExceedingLimit = groupMembershipCount.entrySet().stream()
                .filter(entry -> entry.getValue() > options.getLargeGroupLimit())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        assertTrue(state.getGroupsExceedingThreshold().containsAll(expectedGroupsExceedingThreshold));
        assertTrue(state.getGroupsExceedingLimit().containsAll(expectedGroupsExceedingLimit));
    }

    private void verifyActor(
            String actorCrn, String workloadUsername,
            Set<String> workloadUsersWithAccess,
            Collection<String> actualGroups,
            WorkloadCredential workloadCredential,
            UserMetadata userMetadata) {
        if (testData.memberCrnToActorRights.get(actorCrn).get(UserSyncConstants.RIGHTS.get(0))) {
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
            assertEquals(actorCrn, userMetadata.getCrn());
            assertEquals(workloadCredential.getVersion(), userMetadata.getWorkloadCredentialsVersion());
        } else {
            assertFalse(workloadUsersWithAccess.contains(workloadUsername));
            assertNull(workloadCredential);
            assertNull(userMetadata);
        }
    }

    private void verifyWorkloadCredential(String actorCrn, WorkloadCredential workloadCredential) {
        assertNotNull(workloadCredential);
        UserManagementProto.GetActorWorkloadCredentialsResponse expected =
                testData.memberCrnToWorkloadCredentials.get(actorCrn);
        assertEquals(expected.getPasswordHash(), workloadCredential.getHashedPassword());
        assertEquals(expected.getKerberosKeysList(), workloadCredential.getKeys());
        assertEquals(expected.getSshPublicKeyList(), workloadCredential.getSshPublicKeys());
        assertEquals(expected.getWorkloadCredentialsVersion(), workloadCredential.getVersion());
    }

    private static String createEnvironmentCrn() {
        return CrnTestUtil.getEnvironmentCrnBuilder()
                .setAccountId(ACCOUNT_ID)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    public static class UserSyncTestData {
        // groups
        List<UserManagementProto.Group> testGroups = createGroups("testGroup", 10);

        List<UserManagementProto.Group> largeGroups = createGroups("largeGroup", 1);

        List<UserManagementProto.Group> groups = Stream.concat(testGroups.stream(), largeGroups.stream()).collect(Collectors.toList());

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
        List<UserManagementProto.User> users = createUsers("testUser", 20);

        List<UserManagementProto.MachineUser> machineUsers = createMachineUsers(20);

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
                    .setWorkloadCredentialsVersion(randomNonNegativeLong())
                    .build();
        }

        private Map<String, Map<String, Boolean>> createActorRights(List<String> allActorCrns) {
            return allActorCrns.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            actor -> createRandomBooleans(UserSyncConstants.RIGHTS)));
        }

        private Map<String, Map<String, Boolean>> createGroupMembership(List<String> allActorCrns) {
            List<String> testGroupCrns = testGroups.stream()
                    .map(UserManagementProto.Group::getCrn)
                    .collect(Collectors.toList());
            List<String> largeGroupCrns = largeGroups.stream()
                    .map(UserManagementProto.Group::getCrn)
                    .collect(Collectors.toList());
            return allActorCrns.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            actor ->
                                Stream.concat(createRandomBooleans(testGroupCrns).entrySet().stream(),
                                        createBooleansMap(largeGroupCrns, true).entrySet().stream())
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
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

        private <U> Map<U, Boolean> createBooleansMap(List<U> keys, boolean value) {
            return keys.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            right -> value));
        }

        private List<UserManagementProto.User> createUsers(String userNameBasis, int numUsers) {
            return IntStream.range(0, numUsers)
                    .mapToObj(i -> {
                        return UserManagementProto.User.newBuilder()
                                .setFirstName(RandomStringUtils.randomAlphabetic(10))
                                .setLastName(RandomStringUtils.randomAlphabetic(10))
                                .setCrn(CrnTestUtil.getUserCrnBuilder()
                                        .setAccountId(ACCOUNT_ID)
                                        .setResource(UUID.randomUUID().toString())
                                        .build()
                                        .toString())
                                .setWorkloadUsername(userNameBasis + i)
                                .addCloudIdentities(createCloudIdentity())
                                .setState(UserManagementProto.ActorState.Value.ACTIVE)
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
                                .setCrn(CrnTestUtil.getMachineUserCrnBuilder()
                                        .setAccountId(ACCOUNT_ID)
                                        .setResource(UUID.randomUUID().toString())
                                        .build()
                                        .toString())
                                .setWorkloadUsername(RandomStringUtils.randomAlphabetic(10))
                                .addCloudIdentities(createCloudIdentity())
                                .setState(UserManagementProto.ActorState.Value.ACTIVE)
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
                                .setCrn(CrnTestUtil.getGroupCrnBuilder()
                                        .setAccountId(ACCOUNT_ID)
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

        private static long randomNonNegativeLong() {
            long val = RANDOM.nextLong();
            return val == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(val);
        }
    }
}