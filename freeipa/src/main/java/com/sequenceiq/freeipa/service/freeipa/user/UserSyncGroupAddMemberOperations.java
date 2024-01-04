package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;

@Component
public class UserSyncGroupAddMemberOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncGroupAddMemberOperations.class);

    @Inject
    private UserSyncOperations operations;

    public void addMembersToSmallGroups(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Multimap<String, String> groupMembershipToAdd,
                                        Set<String> largeGroupNames, BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> smallGroups = groupMembershipToAdd.entries().stream()
                .filter(entry -> !largeGroupNames.contains(entry.getKey()))
                .collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, HashMultimap::create));
        operations.addUsersToGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, smallGroups, warnings);
    }

    public void addMembersToLargeGroups(FreeIpaClient freeIpaClient, Multimap<String, String> groupMembershipToAdd,
                                        Set<String> largeGroupNames, BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        if (!largeGroupNames.isEmpty()) {
            LOGGER.debug("group membership addition for large groups will not be batched: {}", largeGroupNames);
            Multimap<String, String> largeGroups = groupMembershipToAdd.entries().stream()
                    .filter(entry -> largeGroupNames.contains(entry.getKey()))
                    .collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, HashMultimap::create));
            operations.addUsersToGroups(false, freeIpaClient, largeGroups, warnings);
        }
    }

}
