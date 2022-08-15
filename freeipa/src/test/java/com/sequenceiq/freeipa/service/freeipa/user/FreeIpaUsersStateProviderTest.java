package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.client.FreeIpaChecks.IPA_PROTECTED_USERS;
import static com.sequenceiq.freeipa.client.FreeIpaChecks.IPA_UNMANAGED_GROUPS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.UserMetadataConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

@ExtendWith(MockitoExtension.class)
class FreeIpaUsersStateProviderTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = CrnTestUtil.getUserCrnBuilder()
            .setAccountId(ACCOUNT_ID)
            .setResource(UUID.randomUUID().toString())
            .build()
            .toString();

    private static final boolean USER_ENABLED = false;

    private static final boolean USER_DISABLED = true;

    private static final List<String> SPLIT_REQUEST_RESOURCE_PREFIXES = List.of(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f");

    @InjectMocks
    FreeIpaUsersStateProvider underTest;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private Stack stack;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private UserMetadataConverter userMetadataConverter;

    @Test
    void testGetUserState() throws Exception {
        List<String> user1GroupNames = List.of("group1", "group2");
        List<String> user2GroupNames = List.of("group2", "group3", IPA_UNMANAGED_GROUPS.get(0));
        List<String> ipaOnlyUserGroupNames = List.of("dont_include");
        List<String> groupsWithoutMembers = List.of("group4");
        Map<String, Pair<List<String>, Optional<UserMetadata>>> users = Map.of(
                "user1", Pair.of(user1GroupNames, Optional.empty()),
                "user2", Pair.of(user2GroupNames, Optional.of(new UserMetadata("user2-crn", 1L))),
                IPA_PROTECTED_USERS.get(0), Pair.of(ipaOnlyUserGroupNames, Optional.empty())
        );

        Set<com.sequenceiq.freeipa.client.model.User> usersFindAll = users.entrySet().stream()
                .map(entry -> createIpaUser(entry.getKey(), entry.getValue().getLeft()))
                .collect(Collectors.toSet());

        Set<com.sequenceiq.freeipa.client.model.Group> groupsFindAll = Stream.of(user1GroupNames.stream(),
                user2GroupNames.stream(), groupsWithoutMembers.stream(), IPA_UNMANAGED_GROUPS.stream())
                .flatMap(groupName -> groupName)
                .map(this::createIpaGroup)
                .collect(Collectors.toSet());

        when(freeIpaClient.userFindAll(Optional.empty(), Map.of("all", true))).thenReturn(usersFindAll);
        when(freeIpaClient.groupFindAll()).thenReturn(groupsFindAll);

        Set<String> expectedUsers = users.keySet().stream()
                .filter(user -> !IPA_PROTECTED_USERS.contains(user))
                .collect(Collectors.toSet());

        Set<String> expectedGroups = groupsFindAll.stream()
                .map(com.sequenceiq.freeipa.client.model.Group::getCn)
                .filter(groupName -> !IPA_UNMANAGED_GROUPS.contains(groupName))
                .collect(Collectors.toSet());

        Map<String, UserMetadata> expectedUserMetadata = Maps.newHashMap();
        expectedUsers.forEach(username -> {
            Optional<UserMetadata> userMetadata = users.get(username).getRight();
            doReturn(userMetadata).when(userMetadataConverter).toUserMetadata(argThat(arg -> username.equals(arg.getUid())));
            userMetadata.ifPresent(meta -> expectedUserMetadata.put(username, meta));
        });

        UsersState ipaState = underTest.getUsersState(freeIpaClient, false);

        for (FmsUser fmsUser : ipaState.getUsers()) {
            assertTrue(expectedUsers.contains(fmsUser.getName()));
            expectedUsers.remove(fmsUser.getName());
        }
        assertTrue(expectedUsers.isEmpty());

        for (FmsGroup fmsGroup : ipaState.getGroups()) {
            assertTrue(expectedGroups.contains(fmsGroup.getName()));
            expectedGroups.remove(fmsGroup.getName());
        }
        assertTrue(expectedGroups.isEmpty());

        assertEquals(expectedUserMetadata, ipaState.getUserMetadataMap());
    }

    @Test
    void testGetUserStateSplitRequests() throws Exception {
        List<String> userGroupNames = List.of("group1", "group2", "group3", IPA_UNMANAGED_GROUPS.get(0));

        Set<com.sequenceiq.freeipa.client.model.User> allUsers = new HashSet<>();
        Map<String, UserMetadata> expectedUserMetadata = Maps.newHashMap();

        for (String prefix : SPLIT_REQUEST_RESOURCE_PREFIXES) {
            UserMetadata userMetadata = new UserMetadata(createUserCrn(prefix), 1L);
            String username = prefix + UUID.randomUUID().toString();
            doReturn(Optional.of(userMetadata)).when(userMetadataConverter).toUserMetadata(argThat(arg -> username.equals(arg.getUid())));
            expectedUserMetadata.put(username, userMetadata);
            com.sequenceiq.freeipa.client.model.User user = createIpaUser(username, userGroupNames);
            when(freeIpaClient.userFindAll(Optional.of(":user:" + prefix),
                    Map.of("all", true))).thenReturn(Set.of(user));
            allUsers.add(user);
        }
        UserMetadata userMetadata = new UserMetadata(createMachineUserCrn(), 1L);
        String username = "macusername/" + UUID.randomUUID().toString();
        doReturn(Optional.of(userMetadata)).when(userMetadataConverter).toUserMetadata(argThat(arg -> username.equals(arg.getUid())));
        expectedUserMetadata.put(username, userMetadata);
        com.sequenceiq.freeipa.client.model.User user = createIpaUser(username, userGroupNames);
        when(freeIpaClient.userFindAll(Optional.of(":machineUser:"), Map.of("all", true))).thenReturn(Set.of(user));
        allUsers.add(user);

        Set<com.sequenceiq.freeipa.client.model.Group> groupsFindAll = userGroupNames.stream()
                .map(this::createIpaGroup)
                .collect(Collectors.toSet());
        when(freeIpaClient.groupFindAll()).thenReturn(groupsFindAll);

        UsersState ipaState = underTest.getUsersState(freeIpaClient, true);

        List<String> allUserNames = allUsers.stream()
                .map(com.sequenceiq.freeipa.client.model.User::getUid)
                .sorted()
                .collect(Collectors.toList());
        assertEquals(allUserNames.size(), ipaState.getUsers().size());
        for (FmsUser fmsUser : ipaState.getUsers()) {
            assertTrue(allUserNames.contains(fmsUser.getName()));
        }

        assertEquals(expectedUserMetadata, ipaState.getUserMetadataMap());
    }

    @Test
    void testGetFilteredFreeIpaState() throws Exception {
        List<String> user1GroupNames = List.of("group1", "group2");
        List<String> user2GroupNames = List.of("group2", "group3", IPA_UNMANAGED_GROUPS.get(0));
        List<String> groupsWithoutMembers = List.of("group4");

        com.sequenceiq.freeipa.client.model.User user1 = createIpaUser("user1", user1GroupNames);
        String userNotFound = "userNotFound";

        Set<com.sequenceiq.freeipa.client.model.Group> groupsFindAll = Stream.of(user1GroupNames.stream(),
                user2GroupNames.stream(), groupsWithoutMembers.stream(), IPA_UNMANAGED_GROUPS.stream())
                .flatMap(groupName -> groupName)
                .map(this::createIpaGroup)
                .collect(Collectors.toSet());

        JsonRpcClientException jsonRpcException = new JsonRpcClientException(
                FreeIpaErrorCodes.NOT_FOUND.getValue(), "group not found", null);
        FreeIpaClientException notFoundException = new FreeIpaClientException(
                "Invoke FreeIPA failed", jsonRpcException);

        when(freeIpaClient.userShow(user1.getUid())).thenReturn(user1);
        when(freeIpaClient.userShow(userNotFound)).thenThrow(notFoundException);
        when(freeIpaClient.groupFindAll()).thenReturn(groupsFindAll);

        Set<String> expectedUsers = Sets.newHashSet(user1.getUid());

        Set<String> expectedGroups = groupsFindAll.stream()
                .map(com.sequenceiq.freeipa.client.model.Group::getCn)
                .filter(groupName -> !IPA_UNMANAGED_GROUPS.contains(groupName))
                .collect(Collectors.toSet());

        UserMetadata user1Metadata = new UserMetadata("user1-crn", 1L);
        doReturn(Optional.of(user1Metadata)).when(userMetadataConverter).toUserMetadata(argThat(arg -> user1.getUid().equals(arg.getUid())));
        Map<String, UserMetadata> expectedUserMetadata = Map.of(user1.getUid(), user1Metadata);

        UsersState ipaState = underTest.getFilteredFreeIpaState(freeIpaClient, Set.of(user1.getUid(), userNotFound));

        for (FmsUser fmsUser : ipaState.getUsers()) {
            assertTrue(expectedUsers.contains(fmsUser.getName()));
            expectedUsers.remove(fmsUser.getName());
        }
        assertTrue(expectedUsers.isEmpty());

        for (FmsGroup fmsGroup : ipaState.getGroups()) {
            assertTrue(expectedGroups.contains(fmsGroup.getName()));
            expectedGroups.remove(fmsGroup.getName());
        }
        assertTrue(expectedGroups.isEmpty());

        assertEquals(expectedUserMetadata, ipaState.getUserMetadataMap());

    }

    @Test
    void testGetUserStateUserWithNullGroups() throws Exception {
        String username = "userNull";
        Set<com.sequenceiq.freeipa.client.model.User> usersFindAll = Set.of(createIpaUser(username, null));

        Set<com.sequenceiq.freeipa.client.model.Group> groupsFindAll = IPA_UNMANAGED_GROUPS.stream()
                .map(this::createIpaGroup)
                .collect(Collectors.toSet());

        when(freeIpaClient.userFindAll(Optional.empty(), Map.of("all", true))).thenReturn(usersFindAll);
        when(freeIpaClient.groupFindAll()).thenReturn(groupsFindAll);

        UsersState ipaState = underTest.getUsersState(freeIpaClient, false);

        assertEquals(1, ipaState.getUsers().size());
        FmsUser ipaUser = ipaState.getUsers().asList().get(0);
        assertEquals(username, ipaUser.getName());
    }

    @Test
    void testFromIpaUserEnabled() {
        com.sequenceiq.freeipa.client.model.User ipaUser =
                createIpaUser("uid", List.of("group1", "group2"), USER_ENABLED);

        FmsUser fmsUser = underTest.fromIpaUser(ipaUser, Optional.empty());

        assertEquals(fmsUser.getName(), ipaUser.getUid());
        assertEquals(fmsUser.getLastName(), ipaUser.getSn());
        assertEquals(fmsUser.getFirstName(), ipaUser.getGivenname());
        assertEquals(fmsUser.getState(), FmsUser.State.ENABLED);
        assertEquals(null, fmsUser.getCrn());
    }

    @Test
    void testFromIpaUserDisabled() {
        com.sequenceiq.freeipa.client.model.User ipaUser =
                createIpaUser("uid", List.of("group1", "group2"), USER_DISABLED);

        FmsUser fmsUser = underTest.fromIpaUser(ipaUser, Optional.empty());

        assertEquals(fmsUser.getName(), ipaUser.getUid());
        assertEquals(fmsUser.getLastName(), ipaUser.getSn());
        assertEquals(fmsUser.getFirstName(), ipaUser.getGivenname());
        assertEquals(fmsUser.getState(), FmsUser.State.DISABLED);
        assertEquals(null, fmsUser.getCrn());
    }

    @Test
    void testFromIpaUserWithCrn() {
        com.sequenceiq.freeipa.client.model.User ipaUser =
                createIpaUser("uid", List.of("group1", "group2"), USER_ENABLED);
        UserMetadata userMetadata = new UserMetadata(USER_CRN, 1L);

        FmsUser fmsUser = underTest.fromIpaUser(ipaUser, Optional.of(userMetadata));

        assertEquals(fmsUser.getName(), ipaUser.getUid());
        assertEquals(fmsUser.getLastName(), ipaUser.getSn());
        assertEquals(fmsUser.getFirstName(), ipaUser.getGivenname());
        assertEquals(fmsUser.getState(), FmsUser.State.ENABLED);
        assertEquals(USER_CRN, fmsUser.getCrn());
    }

    @Test
    void testFromIpaGroup() {
        com.sequenceiq.freeipa.client.model.Group ipaGroup = createIpaGroup("cn");

        FmsGroup fmsGroup = underTest.fromIpaGroup(ipaGroup);

        assertEquals(fmsGroup.getName(), ipaGroup.getCn());
    }

    private String createUserCrn(String resourcePrefix) {
        return CrnTestUtil.getUserCrnBuilder()
                .setAccountId(ACCOUNT_ID)
                .setResource(resourcePrefix + UUID.randomUUID().toString())
                .build()
                .toString();
    }

    private String createMachineUserCrn() {
        return CrnTestUtil.getMachineUserCrnBuilder()
                .setAccountId(ACCOUNT_ID)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    private com.sequenceiq.freeipa.client.model.User createIpaUser(String uid, List<String> memberOfGroup) {
        return createIpaUser(uid, memberOfGroup, USER_ENABLED);
    }

    private com.sequenceiq.freeipa.client.model.User createIpaUser(
            String uid, List<String> memberOfGroup, boolean disabled) {
        com.sequenceiq.freeipa.client.model.User ipaUser = new com.sequenceiq.freeipa.client.model.User();
        ipaUser.setUid(uid);
        ipaUser.setDn(UUID.randomUUID().toString());
        ipaUser.setSn(UUID.randomUUID().toString());
        ipaUser.setGivenname(UUID.randomUUID().toString());
        ipaUser.setMemberOfGroup(memberOfGroup);
        ipaUser.setNsAccountLock(disabled);
        return ipaUser;
    }

    private com.sequenceiq.freeipa.client.model.Group createIpaGroup(String cn) {
        com.sequenceiq.freeipa.client.model.Group ipaGroup = new com.sequenceiq.freeipa.client.model.Group();
        ipaGroup.setCn(cn);
        return ipaGroup;
    }
}