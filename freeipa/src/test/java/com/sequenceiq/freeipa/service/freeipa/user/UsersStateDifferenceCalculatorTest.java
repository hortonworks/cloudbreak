package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

class UsersStateDifferenceCalculatorTest {

    @Test
    void testCalculateUsersToAdd() {
        FmsUser userUms1 = new FmsUser().withName("userUms").withFirstName("user1");
        FmsUser userUms2 = new FmsUser().withName("userUms").withFirstName("user2");
        FmsUser userDisabledUms = new FmsUser().withName("userDisabledUms")
                .withState(FmsUser.State.DISABLED);
        FmsUser userProtected = new FmsUser().withName(FreeIpaChecks.IPA_PROTECTED_USERS.get(0));
        FmsUser userBothUms = new FmsUser().withName("userBoth");
        FmsUser userBothIpa = new FmsUser().withName("userBoth");
        FmsUser userIpa = new FmsUser().withName("userIPA");
        FmsUser userSameStateUms = new FmsUser().withName("userSameState")
                .withState(FmsUser.State.DISABLED);
        FmsUser userSameStateIpa = new FmsUser().withName("userSameState")
                .withState(FmsUser.State.DISABLED);
        FmsUser userDifferentStateUms = new FmsUser().withName("userDifferentState")
                .withState(FmsUser.State.ENABLED);
        FmsUser userDifferentStateIpa = new FmsUser().withName("userDifferentState")
                .withState(FmsUser.State.DISABLED);

        UmsUsersState umsUsersState = new UmsUsersState.Builder()
                .setUsersState(new UsersState.Builder()
                        .addUser(userUms1)
                        .addUser(userUms2)
                        .addUser(userDisabledUms)
                        .addUser(userProtected)
                        .addUser(userBothUms)
                        .addUser(userSameStateUms)
                        .addUser(userDifferentStateUms)
                        .build())
                .build();

        UsersState ipaUsersState = new UsersState.Builder()
                .addUser(userBothIpa)
                .addUser(userIpa)
                .addUser(userSameStateIpa)
                .addUser(userDifferentStateIpa)
                .build();

        ImmutableSet<FmsUser> usersToAdd = new UserStateDifferenceCalculator().calculateUsersToAdd(umsUsersState, ipaUsersState);

        // the user that exists only in the UMS will be added
        assertThat(usersToAdd).containsAnyOf(userUms1, userUms2);
        assertTrue(usersToAdd.contains(userDisabledUms));
        // protected users will be ignored
        assertFalse(usersToAdd.contains(userProtected));
        // users that exist in both or only in ipa will not be added
        assertFalse(usersToAdd.contains(userBothUms));
        assertFalse(usersToAdd.contains(userIpa));
        assertFalse(usersToAdd.contains(userSameStateUms));
        assertFalse(usersToAdd.contains(userDifferentStateUms));
    }

    @Test
    void testCalculateUsersToRemove() {
        FmsUser userUms = new FmsUser().withName("userUms");
        FmsUser userBoth = new FmsUser().withName("userBoth");
        FmsUser userIPA = new FmsUser().withName("userIPA");
        FmsUser userIPA2 = new FmsUser().withName("userIPA2");
        FmsUser userProtected = new FmsUser().withName(FreeIpaChecks.IPA_PROTECTED_USERS.get(0));

        UmsUsersState umsUsersState = new UmsUsersState.Builder()
                .setUsersState(new UsersState.Builder()
                        .addUser(userUms)
                        .addMemberToGroup(CDP_USERSYNC_INTERNAL_GROUP, userUms.getName())
                        .addUser(userBoth)
                        .addMemberToGroup(CDP_USERSYNC_INTERNAL_GROUP, userBoth.getName())
                        .build())
                .build();

        UsersState ipaUsersState = new UsersState.Builder()
                .addUser(userBoth)
                .addMemberToGroup(CDP_USERSYNC_INTERNAL_GROUP, userBoth.getName())
                .addUser(userIPA)
                .addMemberToGroup(CDP_USERSYNC_INTERNAL_GROUP, userIPA.getName())
                .addUser(userIPA2)
                .addUser(userProtected)
                .addMemberToGroup(CDP_USERSYNC_INTERNAL_GROUP, userProtected.getName())
                .build();

        ImmutableSet<String> usersToRemove = new UserStateDifferenceCalculator().calculateUsersToRemove(umsUsersState, ipaUsersState);

        // the users that exists only in IPA that are members of the CDP_USERSYNC_INTERNAL_GROUP will be removed
        assertTrue(usersToRemove.contains(userIPA.getName()));
        // protected users will be ignored
        assertFalse(usersToRemove.contains(userProtected.getName()));
        // users that exist only in ums, exist in both ums and ipa, or are not members of CDP_USERSYNC_INTERNAL_GROUP will not be removed
        assertFalse(usersToRemove.contains(userUms.getName()));
        assertFalse(usersToRemove.contains(userBoth.getName()));
        assertFalse(usersToRemove.contains(userIPA2.getName()));
    }

    @Test
    void testCalculateGroupsToAdd() {
        FmsGroup groupUms = new FmsGroup().withName("groupUms");
        FmsGroup groupWag = new FmsGroup().withName("groupWag");
        FmsGroup groupBoth = new FmsGroup().withName("groupBoth");
        FmsGroup groupIPA = new FmsGroup().withName("groupIPA");
        FmsGroup groupProtected = new FmsGroup().withName(FreeIpaChecks.IPA_PROTECTED_GROUPS.get(0));

        UmsUsersState umsUsersState = new UmsUsersState.Builder()
                .setUsersState(new UsersState.Builder()
                        .addGroup(groupUms)
                        .addGroup(groupBoth)
                        .addGroup(groupProtected)
                        .build())
                .setWorkloadAdministrationGroups(Set.of(groupWag))
                .build();

        UsersState ipaUsersState = new UsersState.Builder()
                .addGroup(groupBoth)
                .addGroup(groupIPA)
                .build();

        ImmutableSet<FmsGroup> groupsToAdd = new UserStateDifferenceCalculator().calculateGroupsToAdd(umsUsersState, ipaUsersState);

        // group that exists only in UMS will be added
        assertTrue(groupsToAdd.contains(groupUms));
        // protected groups will be ignored
        assertFalse(groupsToAdd.contains(groupProtected));
        // extra wags will not be added
        assertFalse(groupsToAdd.contains(groupWag));
        // groups that exist in both or only ipa will not be added
        assertFalse(groupsToAdd.contains(groupBoth));
        assertFalse(groupsToAdd.contains(groupIPA));
    }

    @Test
    void testCalculateGroupsToRemove() {
        FmsGroup groupUms = new FmsGroup().withName("groupUms");
        FmsGroup groupWag = new FmsGroup().withName("groupWag");
        FmsGroup groupBoth = new FmsGroup().withName("groupBoth");
        FmsGroup groupIPA = new FmsGroup().withName("groupIPA");
        FmsGroup groupProtected = new FmsGroup().withName(FreeIpaChecks.IPA_PROTECTED_GROUPS.get(0));

        UmsUsersState umsUsersState = new UmsUsersState.Builder()
                .setUsersState(new UsersState.Builder()
                        .addGroup(groupUms)
                        .addGroup(groupBoth)
                        .build())
                .setWorkloadAdministrationGroups(Set.of(groupWag))
                .build();

        UsersState ipaUsersState = new UsersState.Builder()
                .addGroup(groupBoth)
                .addGroup(groupIPA)
                .addGroup(groupWag)
                .addGroup(groupProtected)
                .build();

        ImmutableSet<FmsGroup> groupsToRemove = new UserStateDifferenceCalculator().calculateGroupsToRemove(umsUsersState, ipaUsersState);

        // group that exists only in IPA will be removed
        assertTrue(groupsToRemove.contains(groupIPA));
        // group that exists in IPA will not be removed if the wag still exists in control plane
        // even if the group is not calculated to be synced
        assertFalse(groupsToRemove.contains(groupWag));
        // protected groups will not be removed
        assertFalse(groupsToRemove.contains(groupProtected));
        // groups that exist in both or only ums will not be removed
        assertFalse(groupsToRemove.contains(groupBoth));
        assertFalse(groupsToRemove.contains(groupUms));
    }

    @Test
    void testCalculateGroupMembershipsToAdd() {
        int groupSizeLimit = 10;

        String group = "group";
        String unmanagedGroup = FreeIpaChecks.IPA_UNMANAGED_GROUPS.get(0);
        String largeGroup = "largeGroup";

        String userUms = "userUms";
        String userBoth = "userBoth";
        String userIPA = "userIPA";

        String userLargeGroupBase = "userLargeGroup";

        UsersState.Builder usersStateBuilder = UsersState.newBuilder()
                .addMemberToGroup(group, userUms)
                .addMemberToGroup(group, userBoth)
                .addMemberToGroup(unmanagedGroup, userUms);

        for (int i = 0; i <= groupSizeLimit; ++i) {
            usersStateBuilder.addMemberToGroup(largeGroup, userLargeGroupBase + i);
        }

        UmsUsersState umsUsersState = new UmsUsersState.Builder()
                .setUsersState(usersStateBuilder.build())
                .build();

        UsersState ipaUsersState = new UsersState.Builder()
                .addMemberToGroup(group, userBoth)
                .addMemberToGroup(group, userIPA)
                .build();

        UserSyncOptions options = UserSyncOptions.newBuilder()
                .enforceGroupMembershipLimitEnabled(false)
                .largeGroupLimit(groupSizeLimit)
                .build();

        Multimap<String, String> warnings = ArrayListMultimap.create();

        ImmutableMultimap<String, String> groupMembershipsToAdd = new UserStateDifferenceCalculator()
                .calculateGroupMembershipToAdd(umsUsersState, ipaUsersState, options, warnings::put);

        // members will be added to group that exists only in UMS
        assertTrue(groupMembershipsToAdd.get(group).contains(userUms));
        // unmanaged groups will be ignored
        assertFalse(groupMembershipsToAdd.get(unmanagedGroup).contains(userUms));
        // members will not be added to groups that exist in both or only ipa
        assertFalse(groupMembershipsToAdd.get(group).contains(userBoth));
        assertFalse(groupMembershipsToAdd.get(group).contains(userIPA));
        // limit + 1 users added to large group
        assertNotNull(groupMembershipsToAdd.get(largeGroup));
        assertEquals(groupSizeLimit + 1, groupMembershipsToAdd.get(largeGroup).size());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void testCalculateGroupMembershipsToAddExceedsLimit() {
        int groupSizeLimit = 10;

        String group = "group";
        String unmanagedGroup = FreeIpaChecks.IPA_UNMANAGED_GROUPS.get(0);
        String largeGroup = "largeGroup";
        String allowedLargeGroup = UserSyncConstants.ENV_ASSIGNEES_GROUP_PREFIX + UUID.randomUUID().toString();

        String userUms = "userUms";
        String userBoth = "userBoth";
        String userIPA = "userIPA";

        String userLargeGroupBase = "userLargeGroup";

        UsersState.Builder usersStateBuilder = UsersState.newBuilder()
                .addMemberToGroup(group, userUms)
                .addMemberToGroup(group, userBoth)
                .addMemberToGroup(unmanagedGroup, userUms);

        for (int i = 0; i <= groupSizeLimit; ++i) {
            usersStateBuilder.addMemberToGroup(largeGroup, userLargeGroupBase + i);
            usersStateBuilder.addMemberToGroup(allowedLargeGroup, userLargeGroupBase + i);
        }

        UmsUsersState umsUsersState = new UmsUsersState.Builder()
                .setUsersState(usersStateBuilder.build())
                .setGroupsExceedingLimit(Set.of(largeGroup))
                .build();

        UsersState ipaUsersState = new UsersState.Builder()
                .addMemberToGroup(group, userBoth)
                .addMemberToGroup(group, userIPA)
                .build();

        UserSyncOptions options = UserSyncOptions.newBuilder()
                .enforceGroupMembershipLimitEnabled(true)
                .largeGroupLimit(groupSizeLimit)
                .build();

        Multimap<String, String> warnings = ArrayListMultimap.create();

        ImmutableMultimap<String, String> groupMembershipsToAdd = new UserStateDifferenceCalculator()
                .calculateGroupMembershipToAdd(umsUsersState, ipaUsersState, options, warnings::put);

        // members will be added to group that exists only in UMS
        assertTrue(groupMembershipsToAdd.get(group).contains(userUms));
        // unmanaged groups will be ignored
        assertFalse(groupMembershipsToAdd.get(unmanagedGroup).contains(userUms));
        // members will not be added to groups that exist in both or only ipa
        assertFalse(groupMembershipsToAdd.get(group).contains(userBoth));
        assertFalse(groupMembershipsToAdd.get(group).contains(userIPA));
        // no members added to large group
        assertFalse(groupMembershipsToAdd.containsKey(largeGroup));
        assertEquals(1, warnings.size());
        // allowed large group should be in the difference
        assertTrue(groupMembershipsToAdd.containsKey(allowedLargeGroup));
    }

    @Test
    void testCalculateGroupMembershipsToRemove() {
        String group = "group";
        String unmanagedGroup = FreeIpaChecks.IPA_UNMANAGED_GROUPS.get(0);

        String userUms = "userUms";
        String userBoth = "userBoth";
        String userIPA = "userIPA";
        String userNonUsersync = "userNonUsersync";

        UmsUsersState umsUsersState = new UmsUsersState.Builder()
                .setUsersState(new UsersState.Builder()
                        .addMemberToGroup(group, userUms)
                        .addMemberToGroup(group, userBoth)
                        .build())
                .build();

        UsersState ipaUsersState = new UsersState.Builder()
                .addMemberToGroup(group, userBoth)
                .addMemberToGroup(CDP_USERSYNC_INTERNAL_GROUP, userBoth)
                .addMemberToGroup(group, userIPA)
                .addMemberToGroup(CDP_USERSYNC_INTERNAL_GROUP, userIPA)
                .addMemberToGroup(unmanagedGroup, userIPA)
                .addMemberToGroup(group, userNonUsersync)
                .build();

        ImmutableMultimap<String, String> groupMembershipsToRemove = new UserStateDifferenceCalculator()
                .calculateGroupMembershipToRemove(umsUsersState, ipaUsersState);

        // group that exists only in IPA will be removed
        assertTrue(groupMembershipsToRemove.get(group).contains(userIPA));
        // unmanaged groups will be ignored
        assertFalse(groupMembershipsToRemove.get(unmanagedGroup).contains(userIPA));
        // groups that exist in both or only ums will not be removed
        assertFalse(groupMembershipsToRemove.get(group).contains(userBoth));
        assertFalse(groupMembershipsToRemove.get(group).contains(userUms));
        // user that is not in CDP_USERSYNC_INTERNAL_GROUP will not be removed
        assertFalse(groupMembershipsToRemove.get(group).contains(userNonUsersync));
    }

    @Test
    void testCalculateUsersToEnable() {
        UsersState.Builder umsUsersStateBuilder = new UsersState.Builder();
        UsersState.Builder ipaUsersStateBuilder = new UsersState.Builder();

        addUserWithState("user1UmsEnabled", umsUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user2UmsDisabled", umsUsersStateBuilder, FmsUser.State.DISABLED);
        addUserWithState("user3UmsEnabledIpaEnabled", umsUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user3UmsEnabledIpaEnabled", ipaUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user4UmsEnabledIpaDisabled", umsUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user4UmsEnabledIpaDisabled", ipaUsersStateBuilder, FmsUser.State.DISABLED);
        addUserWithState("user5UmsDisabledIpaEnabled", umsUsersStateBuilder, FmsUser.State.DISABLED);
        addUserWithState("user5UmsDisabledIpaEnabled", ipaUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user6UmsDisabledIpaDisabled", umsUsersStateBuilder, FmsUser.State.DISABLED);
        addUserWithState("user6UmsDisabledIpaDisabled", ipaUsersStateBuilder, FmsUser.State.DISABLED);
        addUserWithState("user7IpaEnabled", ipaUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user8IpaDisabled", ipaUsersStateBuilder, FmsUser.State.DISABLED);
        // also check that we don't change a protected user
        addUserWithState(FreeIpaChecks.IPA_PROTECTED_USERS.get(0), umsUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState(FreeIpaChecks.IPA_PROTECTED_USERS.get(0), ipaUsersStateBuilder, FmsUser.State.DISABLED);

        UmsUsersState umsUsersState = new UmsUsersState.Builder()
                .setUsersState(umsUsersStateBuilder.build())
                .build();

        UsersState ipaUsersState = ipaUsersStateBuilder.build();

        ImmutableSet<String> usersToEnable = new UserStateDifferenceCalculator().calculateUsersToEnable(umsUsersState, ipaUsersState);

        // the users that are enabled in UMS but disabled in IPA are enabled
        // new users added to IPA do not need to be enabled
        assertFalse(usersToEnable.contains("user1UmsEnabled"));
        assertFalse(usersToEnable.contains("user2UmsDisabled"));
        assertFalse(usersToEnable.contains("user3UmsEnabledIpaEnabled"));
        assertTrue(usersToEnable.contains("user4UmsEnabledIpaDisabled"));
        assertFalse(usersToEnable.contains("user5UmsDisabledIpaEnabled"));
        assertFalse(usersToEnable.contains("user6UmsDisabledIpaDisabled"));
        assertFalse(usersToEnable.contains("user7IpaEnabled"));
        assertFalse(usersToEnable.contains("user8IpaDisabled"));
        assertFalse(usersToEnable.contains(FreeIpaChecks.IPA_PROTECTED_USERS.get(0)));
    }

    @Test
    void testCalculateUsersToDisable() {
        UsersState.Builder umsUsersStateBuilder = new UsersState.Builder();
        UsersState.Builder ipaUsersStateBuilder = new UsersState.Builder();

        addUserWithState("user1UmsEnabled", umsUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user2UmsDisabled", umsUsersStateBuilder, FmsUser.State.DISABLED);
        addUserWithState("user3UmsEnabledIpaEnabled", umsUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user3UmsEnabledIpaEnabled", ipaUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user4UmsEnabledIpaDisabled", umsUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user4UmsEnabledIpaDisabled", ipaUsersStateBuilder, FmsUser.State.DISABLED);
        addUserWithState("user5UmsDisabledIpaEnabled", umsUsersStateBuilder, FmsUser.State.DISABLED);
        addUserWithState("user5UmsDisabledIpaEnabled", ipaUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user6UmsDisabledIpaDisabled", umsUsersStateBuilder, FmsUser.State.DISABLED);
        addUserWithState("user6UmsDisabledIpaDisabled", ipaUsersStateBuilder, FmsUser.State.DISABLED);
        addUserWithState("user7IpaEnabled", ipaUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState("user8IpaDisabled", ipaUsersStateBuilder, FmsUser.State.DISABLED);
        // also check that we don't change a protected user
        addUserWithState(FreeIpaChecks.IPA_PROTECTED_USERS.get(0), umsUsersStateBuilder, FmsUser.State.ENABLED);
        addUserWithState(FreeIpaChecks.IPA_PROTECTED_USERS.get(0), ipaUsersStateBuilder, FmsUser.State.DISABLED);

        UmsUsersState umsUsersState = new UmsUsersState.Builder()
                .setUsersState(umsUsersStateBuilder.build())
                .build();

        UsersState ipaUsersState = ipaUsersStateBuilder.build();

        ImmutableSet<String> usersToDisable = new UserStateDifferenceCalculator().calculateUsersToDisable(umsUsersState, ipaUsersState);

        // the users that are disabled in UMS but enabled in IPA are disabled
        // new disabled users added to IPA need to be disabled
        assertFalse(usersToDisable.contains("user1UmsEnabled"));
        assertFalse(usersToDisable.contains("user2UmsDisabled"));
        assertFalse(usersToDisable.contains("user3UmsEnabledIpaEnabled"));
        assertFalse(usersToDisable.contains("user4UmsEnabledIpaDisabled"));
        assertTrue(usersToDisable.contains("user5UmsDisabledIpaEnabled"));
        assertFalse(usersToDisable.contains("user6UmsDisabledIpaDisabled"));
        assertFalse(usersToDisable.contains("user7IpaEnabled"));
        assertFalse(usersToDisable.contains("user8IpaDisabled"));
        assertFalse(usersToDisable.contains(FreeIpaChecks.IPA_PROTECTED_USERS.get(0)));
    }

    private void addUserWithState(String username, UsersState.Builder userStateBuilder, FmsUser.State state) {
        userStateBuilder
                .addUser(new FmsUser().withName(username).withState(state))
                .addMemberToGroup(CDP_USERSYNC_INTERNAL_GROUP, username);
    }

    @Test
    void testCalculateUsersWithCredentialsToUpdateWithUpdateOptimization() {
        testCalculateUsersWithCredentialsToUpdate(true);
    }

    @Test
    void testCalculateUsersWithCredentialsToUpdateWithoutUpdateOptimization() {
        testCalculateUsersWithCredentialsToUpdate(false);
    }

    private void testCalculateUsersWithCredentialsToUpdate(boolean updatedOptimizationEnabled) {
        UmsUsersState.Builder umsUsersStateBuilder = UmsUsersState.newBuilder();
        UsersState.Builder usersStateBuilderForUms = UsersState.newBuilder();
        UsersState.Builder usersStateBuilderForIpa = UsersState.newBuilder();

        FmsUser userUms = addUmsUser("userUms", 1L, umsUsersStateBuilder, usersStateBuilderForUms);

        FmsUser userWithNoIpaMetadata = addUmsUser("userWithNoIpaMetadata", 0L, umsUsersStateBuilder, usersStateBuilderForUms);
        addIpaUser(userWithNoIpaMetadata.getName(), Optional.empty(), usersStateBuilderForIpa);

        FmsUser userWithStaleIpaCredentials = addUmsUser("userWithStaleIpaCredentials", 2L, umsUsersStateBuilder, usersStateBuilderForUms);
        addIpaUser(userWithStaleIpaCredentials.getName(), Optional.of(1L), usersStateBuilderForIpa);

        FmsUser userWithUpToDateIpaCredentials = addUmsUser("userWithUpToDateIpaCredentials", 5L, umsUsersStateBuilder, usersStateBuilderForUms);
        addIpaUser(userWithUpToDateIpaCredentials.getName(), Optional.of(5L), usersStateBuilderForIpa);

        FmsUser userProtected = addUmsUser(FreeIpaChecks.IPA_PROTECTED_USERS.get(0), 0L, umsUsersStateBuilder, usersStateBuilderForUms);
        addIpaUser(userProtected.getName(), Optional.empty(), usersStateBuilderForIpa);

        UmsUsersState umsUsersState = umsUsersStateBuilder.setUsersState(usersStateBuilderForUms.build()).build();
        UsersState ipaUsersState = usersStateBuilderForIpa.build();

        ImmutableSet<String> usersWithCredentialsToUpdate = new UserStateDifferenceCalculator().calculateUsersWithCredentialsToUpdate(
                umsUsersState, ipaUsersState, updatedOptimizationEnabled);

        // User that exists only in UMS requires credentials update
        assertTrue(usersWithCredentialsToUpdate.contains(userUms.getName()));
        // User whose IPA credentials version is unknown requires credentials update
        assertTrue(usersWithCredentialsToUpdate.contains(userWithNoIpaMetadata.getName()));
        // User with stale IPA credentials requires credentials update
        assertTrue(usersWithCredentialsToUpdate.contains(userWithStaleIpaCredentials.getName()));
        // User with up-to-date IPA credentials requires credentials update if update optimization is disabled
        assertEquals(!updatedOptimizationEnabled, usersWithCredentialsToUpdate.contains(userWithUpToDateIpaCredentials.getName()));
        // We never update credentials for protected users
        assertFalse(usersWithCredentialsToUpdate.contains(userProtected.getName()));
    }

    private FmsUser addUmsUser(String username, long umsCredentialsVersion, UmsUsersState.Builder umsStateBuilder,
            UsersState.Builder usersStateBuilder) {
        FmsUser fmsUser = new FmsUser().withName(username);
        usersStateBuilder.addUser(fmsUser);
        umsStateBuilder.addWorkloadCredentials(username, UserSyncTestUtils.createWorkloadCredential("hashedPassword", umsCredentialsVersion));
        return fmsUser;
    }

    private void addIpaUser(String username, Optional<Long> ipaCredentialsVersion, UsersState.Builder usersStateBuilder) {
        FmsUser fmsUser = new FmsUser().withName(username);
        usersStateBuilder.addUser(fmsUser);
        if (ipaCredentialsVersion.isPresent()) {
            String crn = CrnTestUtil.getUserCrnBuilder()
                    .setAccountId(UUID.randomUUID().toString())
                    .setResource(UUID.randomUUID().toString())
                    .build().toString();
            usersStateBuilder.addUserMetadata(username, new UserMetadata(crn, ipaCredentialsVersion.get()));
        }
    }
}