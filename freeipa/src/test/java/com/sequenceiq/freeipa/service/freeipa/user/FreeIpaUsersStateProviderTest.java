package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

@ExtendWith(MockitoExtension.class)
class FreeIpaUsersStateProviderTest {

    @InjectMocks
    FreeIpaUsersStateProvider underTest;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private Stack stack;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    void testGetUserState() throws Exception {
        List<String> user1GroupNames = List.of("group1", "group2");
        List<String> user2GroupNames = List.of("group2", "group3", FreeIpaUsersStateProvider.IPA_ONLY_GROUPS.get(0));
        List<String> ipaOnlyUserGroupNames = List.of("dont_include");
        Map<String, List<String>> users = Map.of(
                "user1", user1GroupNames,
                "user2", user2GroupNames,
                FreeIpaUsersStateProvider.IPA_ONLY_USERS.get(0), ipaOnlyUserGroupNames
        );

        Set<com.sequenceiq.freeipa.client.model.User> usersFindAll = users.entrySet().stream()
                .map(entry -> createIpaUser(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());

        Set<com.sequenceiq.freeipa.client.model.Group> groupsFindAll = Stream.of(user1GroupNames.stream(),
                user2GroupNames.stream(), FreeIpaUsersStateProvider.IPA_ONLY_GROUPS.stream())
                .flatMap(groupName -> groupName)
                .map(this::createIpaGroup)
                .collect(Collectors.toSet());

        when(freeIpaClient.userFindAll()).thenReturn(usersFindAll);
        when(freeIpaClient.groupFindAll()).thenReturn(groupsFindAll);

        UsersState ipaState = underTest.getUsersState(freeIpaClient);

        Set<String> expectedUsers = users.keySet().stream()
                .filter(user -> !FreeIpaUsersStateProvider.IPA_ONLY_USERS.contains(user))
                .collect(Collectors.toSet());

        Set<String> expectedGroups = expectedUsers.stream()
                .flatMap(user -> users.get(user).stream())
                .filter(group -> !FreeIpaUsersStateProvider.IPA_ONLY_GROUPS.contains(group))
                .collect(Collectors.toSet());

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
    }

    @Test
    void testGetUserStateUserWithNullGroups() throws Exception {
        String username = "userNull";
        Set<com.sequenceiq.freeipa.client.model.User> usersFindAll = Set.of(createIpaUser(username, null));

        Set<com.sequenceiq.freeipa.client.model.Group> groupsFindAll = FreeIpaUsersStateProvider.IPA_ONLY_GROUPS.stream()
                .map(this::createIpaGroup)
                .collect(Collectors.toSet());

        when(freeIpaClient.userFindAll()).thenReturn(usersFindAll);
        when(freeIpaClient.groupFindAll()).thenReturn(groupsFindAll);

        UsersState ipaState = underTest.getUsersState(freeIpaClient);

        assertEquals(1, ipaState.getUsers().size());
        FmsUser ipaUser = ipaState.getUsers().asList().get(0);
        assertEquals(username, ipaUser.getName());
    }

    @Test
    void testFromIpaUser() {
        com.sequenceiq.freeipa.client.model.User ipaUser = createIpaUser("uid", List.of("group1", "group2"));

        FmsUser fmsUser = underTest.fromIpaUser(ipaUser);

        assertEquals(fmsUser.getName(), ipaUser.getUid());
        assertEquals(fmsUser.getLastName(), ipaUser.getSn());
        assertEquals(fmsUser.getFirstName(), ipaUser.getGivenname());
    }

    @Test
    void testFromIpaGroup() {
        com.sequenceiq.freeipa.client.model.Group ipaGroup = createIpaGroup("cn");

        FmsGroup fmsGroup = underTest.fromIpaGroup(ipaGroup);

        assertEquals(fmsGroup.getName(), ipaGroup.getCn());
    }

    private com.sequenceiq.freeipa.client.model.User createIpaUser(String uid, List<String> memberOfGroup) {
        com.sequenceiq.freeipa.client.model.User ipaUser = new com.sequenceiq.freeipa.client.model.User();
        ipaUser.setUid(uid);
        ipaUser.setDn(UUID.randomUUID().toString());
        ipaUser.setSn(UUID.randomUUID().toString());
        ipaUser.setGivenname(UUID.randomUUID().toString());
        ipaUser.setMemberOfGroup(memberOfGroup);
        return ipaUser;
    }

    private com.sequenceiq.freeipa.client.model.Group createIpaGroup(String cn) {
        com.sequenceiq.freeipa.client.model.Group ipaGroup = new com.sequenceiq.freeipa.client.model.Group();
        ipaGroup.setCn(cn);
        return ipaGroup;
    }
}