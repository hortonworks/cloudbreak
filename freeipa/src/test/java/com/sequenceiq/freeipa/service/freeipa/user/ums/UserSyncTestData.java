package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class UserSyncTestData {
    private final SecureRandom random = new SecureRandom();

    private final String accountId = UUID.randomUUID().toString();

    private final String actorCrn = Crn.builder(CrnResourceDescriptor.USER)
            .setAccountId(accountId)
            .setResource(UUID.randomUUID().toString())
            .build()
            .toString();

    private final String environmentCrn = createEnvironmentCrn();

    // groups
    private final ImmutableList<UserManagementProto.Group> groups =
            createGroups("testGroup", 10);

    // wags
    private final ImmutableList<UserManagementProto.WorkloadAdministrationGroup> wagsForThisEnvironment =
            createWags(environmentCrn, 10);

    private final ImmutableList<UserManagementProto.WorkloadAdministrationGroup> wagsForOtherEnvironment =
            createWags(createEnvironmentCrn(), 10);

    private final ImmutableList<UserManagementProto.WorkloadAdministrationGroup> allWags =
            ImmutableList.copyOf(Stream.concat(
                    wagsForThisEnvironment.stream(),
                    wagsForOtherEnvironment.stream())
                    .collect(Collectors.toList()));

    // users and machine users
    private final ImmutableList<UserManagementProto.User> users = createUsers("testUser", 10);

    private final ImmutableList<UserManagementProto.MachineUser> machineUsers = createMachineUsers(10);

    private final ImmutableList<String> allActorCrns = ImmutableList.copyOf(Stream.concat(users.stream().map(UserManagementProto.User::getCrn),
            machineUsers.stream().map(UserManagementProto.MachineUser::getCrn))
            .collect(Collectors.toList()));

    // mappings from actor to rights, group memberships, wag memberships, and credentials
    private final ImmutableMap<String, ImmutableMap<String, Boolean>> memberCrnToActorRights =
            createActorRights(allActorCrns);

    private final ImmutableMap<String, ImmutableMap<String, Boolean>> memberCrnToGroupMembership =
            createGroupMembership(allActorCrns);

    private final ImmutableMap<String, ImmutableMap<String, Boolean>> memberCrnToWagMembership =
            createWagMembership(allActorCrns);

    // credentials
    private final ImmutableMap<String, UserManagementProto.GetActorWorkloadCredentialsResponse> memberCrnToWorkloadCredentials =
            createCredentials(allActorCrns);

    // service principals cloud identities
    private final ImmutableList<UserManagementProto.ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities =
            createServicePrincipalCloudIdentities(5);

    public String getAccountId() {
        return accountId;
    }

    public String getActorCrn() {
        return actorCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public ImmutableList<UserManagementProto.Group> getGroups() {
        return groups;
    }

    public ImmutableList<UserManagementProto.WorkloadAdministrationGroup> getWagsForThisEnvironment() {
        return wagsForThisEnvironment;
    }

    public ImmutableList<UserManagementProto.WorkloadAdministrationGroup> getWagsForOtherEnvironment() {
        return wagsForOtherEnvironment;
    }

    public ImmutableList<UserManagementProto.WorkloadAdministrationGroup> getAllWags() {
        return allWags;
    }

    public ImmutableList<UserManagementProto.User> getUsers() {
        return users;
    }

    public ImmutableList<UserManagementProto.MachineUser> getMachineUsers() {
        return machineUsers;
    }

    public ImmutableList<String> getAllActorCrns() {
        return allActorCrns;
    }

    public ImmutableMap<String, ImmutableMap<String, Boolean>> getMemberCrnToActorRights() {
        return memberCrnToActorRights;
    }

    public ImmutableMap<String, ImmutableMap<String, Boolean>> getMemberCrnToGroupMembership() {
        return memberCrnToGroupMembership;
    }

    public ImmutableMap<String, ImmutableMap<String, Boolean>> getMemberCrnToWagMembership() {
        return memberCrnToWagMembership;
    }

    public ImmutableMap<String, UserManagementProto.GetActorWorkloadCredentialsResponse> getMemberCrnToWorkloadCredentials() {
        return memberCrnToWorkloadCredentials;
    }

    public ImmutableList<UserManagementProto.ServicePrincipalCloudIdentities> getServicePrincipalCloudIdentities() {
        return servicePrincipalCloudIdentities;
    }

    private String createEnvironmentCrn() {
        return Crn.builder(CrnResourceDescriptor.ENVIRONMENT)
                .setAccountId(accountId)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    private ImmutableMap<String, UserManagementProto.GetActorWorkloadCredentialsResponse> createCredentials(
            List<String> allActorCrns) {
        Map<String, String> actorCrnToWorkloadUsername = Maps.newHashMap();
        actorCrnToWorkloadUsername.putAll(users.stream()
                .collect(Collectors.toMap(UserManagementProto.User::getCrn,
                        UserManagementProto.User::getWorkloadUsername)));
        actorCrnToWorkloadUsername.putAll(machineUsers.stream()
                .collect(Collectors.toMap(UserManagementProto.MachineUser::getCrn,
                        UserManagementProto.MachineUser::getWorkloadUsername)));
        return ImmutableMap.copyOf(allActorCrns.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        actorCrn -> createActorCredentials(actorCrnToWorkloadUsername.get(actorCrn)))));
    }

    private UserManagementProto.GetActorWorkloadCredentialsResponse createActorCredentials(
            String workloadUsername) {
        return UserManagementProto.GetActorWorkloadCredentialsResponse.newBuilder()
                .setWorkloadUsername(workloadUsername)
                .setPasswordHash(RandomStringUtils.randomAlphabetic(50))
                .setPasswordHashExpirationDate(random.nextLong())
                .addKerberosKeys(UserManagementProto.ActorKerberosKey.newBuilder()
                        .setKeyType(random.nextInt())
                        .setKeyValue(RandomStringUtils.randomAlphabetic(10))
                        .setSaltType(random.nextInt())
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

    private ImmutableMap<String, ImmutableMap<String, Boolean>> createActorRights(List<String> allActorCrns) {
        return ImmutableMap.copyOf(allActorCrns.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        actor -> createRandomBooleans(UserSyncConstants.RIGHTS))));
    }

    private ImmutableMap<String, ImmutableMap<String, Boolean>> createGroupMembership(List<String> allActorCrns) {
        List<String> groupCrns = groups.stream()
                .map(UserManagementProto.Group::getCrn)
                .collect(Collectors.toList());
        return ImmutableMap.copyOf(allActorCrns.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        actor -> createRandomBooleans(groupCrns))));
    }

    private ImmutableMap<String, ImmutableMap<String, Boolean>> createWagMembership(List<String> allActorCrns) {
        List<String> wagNames = allWags.stream()
                .map(UserManagementProto.WorkloadAdministrationGroup::getWorkloadAdministrationGroupName)
                .collect(Collectors.toList());
        return ImmutableMap.copyOf(allActorCrns.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        actor -> createRandomBooleans(wagNames))));
    }

    private <U> ImmutableMap<U, Boolean> createRandomBooleans(List<U> keys) {
        return ImmutableMap.copyOf(keys.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        right -> random.nextBoolean())));
    }

    private ImmutableList<UserManagementProto.User> createUsers(String userNameBasis, int numUsers) {
        return ImmutableList.copyOf(IntStream.range(0, numUsers)
                .mapToObj(i -> {
                    return UserManagementProto.User.newBuilder()
                            .setFirstName(RandomStringUtils.randomAlphabetic(10))
                            .setLastName(RandomStringUtils.randomAlphabetic(10))
                            .setCrn(Crn.builder(CrnResourceDescriptor.USER)
                                    .setAccountId(accountId)
                                    .setResource(UUID.randomUUID().toString())
                                    .build()
                                    .toString())
                            .setWorkloadUsername(userNameBasis + i)
                            .addCloudIdentities(createCloudIdentity())
                            .build();
                })
                .collect(Collectors.toList()));
    }

    private ImmutableList<UserManagementProto.MachineUser> createMachineUsers(int numUsers) {
        return ImmutableList.copyOf(IntStream.range(0, numUsers)
                .mapToObj(i -> {
                    String id = UUID.randomUUID().toString();
                    return UserManagementProto.MachineUser.newBuilder()
                            .setMachineUserId(id)
                            .setMachineUserName(RandomStringUtils.randomAlphabetic(10))
                            .setCrn(Crn.builder(CrnResourceDescriptor.MACHINE_USER)
                                    .setAccountId(accountId)
                                    .setResource(UUID.randomUUID().toString())
                                    .build()
                                    .toString())
                            .setWorkloadUsername(RandomStringUtils.randomAlphabetic(10))
                            .addCloudIdentities(createCloudIdentity())
                            .build();
                })
                .collect(Collectors.toList()));
    }

    private ImmutableList<UserManagementProto.Group> createGroups(String groupNameBasis, int numGroups) {
        return ImmutableList.copyOf(IntStream.range(0, numGroups)
                .mapToObj(i -> {
                    String name = groupNameBasis + i;
                    String id = UUID.randomUUID().toString();
                    return UserManagementProto.Group.newBuilder()
                            .setGroupName(name)
                            .setCrn(Crn.builder(CrnResourceDescriptor.GROUP)
                                    .setAccountId(accountId)
                                    .setResource(name + "/" + id)
                                    .build()
                                    .toString())
                            .build();
                })
                .collect(Collectors.toList()));
    }

    private ImmutableList<UserManagementProto.WorkloadAdministrationGroup> createWags(String environmentCrn, int numWags) {
        return ImmutableList.copyOf(IntStream.range(0, numWags)
                .mapToObj(i -> UserManagementProto.WorkloadAdministrationGroup.newBuilder()
                        .setResource(environmentCrn)
                        .setRightName(UUID.randomUUID().toString())
                        .setWorkloadAdministrationGroupName(UUID.randomUUID().toString())
                        .build())
                .collect(Collectors.toList()));
    }

    private ImmutableList<UserManagementProto.ServicePrincipalCloudIdentities> createServicePrincipalCloudIdentities(int numSP) {
        return ImmutableList.copyOf(IntStream.range(0, numSP)
                .mapToObj(i -> UserManagementProto.ServicePrincipalCloudIdentities.newBuilder()
                        .setServicePrincipal(UUID.randomUUID().toString())
                        .addCloudIdentities(createCloudIdentity())
                        .build())
                .collect(Collectors.toList()));
    }

    private UserManagementProto.CloudIdentity createCloudIdentity() {
        return UserManagementProto.CloudIdentity.newBuilder()
                .setCloudIdentityName(UserManagementProto.CloudIdentityName.newBuilder()
                        .setAzureCloudIdentityName(UserManagementProto.AzureCloudIdentityName.newBuilder()
                                .setObjectId(UUID.randomUUID().toString())
                                .build())
                        .build())
                .setCloudIdentityDomain(UserManagementProto.CloudIdentityDomain.newBuilder()
                        .setEnvironmentCrn(environmentCrn)
                        .setAzureCloudIdentityDomain(UserManagementProto.AzureCloudIdentityDomain.newBuilder()
                                .setAzureAdIdentifier(UUID.randomUUID().toString())
                                .build())
                        .build())
                .build();
    }

    private long randomNonNegativeLong() {
        long val = random.nextLong();
        return val == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(val);
    }
}
