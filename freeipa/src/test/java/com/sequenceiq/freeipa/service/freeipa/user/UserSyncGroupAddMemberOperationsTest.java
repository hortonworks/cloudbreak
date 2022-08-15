package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.freeipa.client.FreeIpaClient;

@ExtendWith(MockitoExtension.class)
class UserSyncGroupAddMemberOperationsTest {

    @Mock
    private UserSyncOperations operations;

    @InjectMocks
    private UserSyncGroupAddMemberOperations underTest;

    @Test
    void addMembersToSmallGroups() throws Exception {
        boolean fmsToFreeipaBatchCallEnabled = true;
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        Multimap<String, String> groupMembershipToAdd = setupGroupMapping(10, 20);
        Set<String> largeGroupNames = Set.of(groupMembershipToAdd.entries().stream()
                .findFirst().get().getKey());
        BiConsumer<String, String> warnings = mock(BiConsumer.class);

        underTest.addMembersToSmallGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, groupMembershipToAdd,
                largeGroupNames, warnings);

        ArgumentCaptor<Multimap<String, String>> groupMembershipCaptor = ArgumentCaptor.forClass(Multimap.class);
        verify(operations).addUsersToGroups(eq(fmsToFreeipaBatchCallEnabled), eq(freeIpaClient),
                groupMembershipCaptor.capture(), eq(warnings));

        Map<String, Collection<String>> smallGroupMemberships = groupMembershipCaptor.getValue().asMap();
        Map<String, Collection<String>> allGroupMemberships = groupMembershipToAdd.asMap();
        assertEquals(allGroupMemberships.keySet().size() - largeGroupNames.size(),
                smallGroupMemberships.keySet().size());
        allGroupMemberships.entrySet().forEach(entry -> {
            String groupName = entry.getKey();
            if (largeGroupNames.contains(groupName)) {
                assertFalse(smallGroupMemberships.containsKey(groupName));
            } else {
                assertTrue(smallGroupMemberships.containsKey(groupName));
                assertEquals(entry.getValue(), smallGroupMemberships.get(groupName));
            }
        });
    }

    @Test
    void addMembersToSmallGroupsNotBatched() throws Exception {
        boolean fmsToFreeipaBatchCallEnabled = false;
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        Multimap<String, String> groupMembershipToAdd = setupGroupMapping(10, 20);
        Set<String> largeGroupNames = Set.of(groupMembershipToAdd.entries().stream()
                .findFirst().get().getKey());
        BiConsumer<String, String> warnings = mock(BiConsumer.class);

        underTest.addMembersToSmallGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, groupMembershipToAdd,
                largeGroupNames, warnings);

        ArgumentCaptor<Multimap<String, String>> groupMembershipCaptor = ArgumentCaptor.forClass(Multimap.class);
        verify(operations).addUsersToGroups(eq(fmsToFreeipaBatchCallEnabled), eq(freeIpaClient),
                groupMembershipCaptor.capture(), eq(warnings));

        Map<String, Collection<String>> smallGroupMemberships = groupMembershipCaptor.getValue().asMap();
        Map<String, Collection<String>> allGroupMemberships = groupMembershipToAdd.asMap();
        assertEquals(allGroupMemberships.keySet().size() - largeGroupNames.size(),
                smallGroupMemberships.keySet().size());
        allGroupMemberships.entrySet().forEach(entry -> {
            String groupName = entry.getKey();
            if (largeGroupNames.contains(groupName)) {
                assertFalse(smallGroupMemberships.containsKey(groupName));
            } else {
                assertTrue(smallGroupMemberships.containsKey(groupName));
                assertEquals(entry.getValue(), smallGroupMemberships.get(groupName));
            }
        });
    }

    @Test
    void addMembersToLargeGroups() throws Exception {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        Multimap<String, String> groupMembershipToAdd = setupGroupMapping(10, 20);
        Set<String> largeGroupNames = Set.of(groupMembershipToAdd.entries().stream()
                .findFirst().get().getKey());
        BiConsumer<String, String> warnings = mock(BiConsumer.class);

        underTest.addMembersToLargeGroups(freeIpaClient, groupMembershipToAdd,
                largeGroupNames, warnings);

        ArgumentCaptor<Multimap<String, String>> groupMembershipCaptor = ArgumentCaptor.forClass(Multimap.class);
        verify(operations).addUsersToGroups(eq(false), eq(freeIpaClient),
                groupMembershipCaptor.capture(), eq(warnings));

        Map<String, Collection<String>> largeGroupMemberships = groupMembershipCaptor.getValue().asMap();
        Map<String, Collection<String>> allGroupMemberships = groupMembershipToAdd.asMap();
        assertEquals(largeGroupNames.size(),
                largeGroupMemberships.keySet().size());
        allGroupMemberships.entrySet().forEach(entry -> {
            String groupName = entry.getKey();
            if (largeGroupNames.contains(groupName)) {
                assertTrue(largeGroupMemberships.containsKey(groupName));
                assertEquals(entry.getValue(), largeGroupMemberships.get(groupName));
            } else {
                assertFalse(largeGroupMemberships.containsKey(groupName));
            }
        });
    }

    private Multimap<String, String> setupGroupMapping(int numGroups, int numPerGroup) {
        Multimap<String, String> groupMapping = HashMultimap.create();
        for (int i = 0; i < numGroups; ++i) {
            String group = "group" + i;
            for (int j = 0; j < numPerGroup; ++j) {
                groupMapping.put(group, "user" + j);
            }
        }
        return groupMapping;
    }
}