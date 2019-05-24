package com.sequenceiq.freeipa.service.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.User;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    public void synchronizeUsers(String accountId, SynchronizeUsersRequest request) throws Exception {
        LOGGER.info("UserService.synchronizeUsers() called");

        Stack stack = stackService.getByAccountIdEnvironmentAndName(accountId, request.getEnvironmentName(), request.getName());

        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);

        // TODO improve exception handling. e.g., group or user add will fail if group or user already exists
        // TODO batch calls for improved performance. i.e., get all members of each group so we call groupAddMember once per group. investigate batch api

        addGroups(freeIpaClient, request.getGroups());

        Set<User> users = request.getUsers();
        addUsers(freeIpaClient, users);

        addUsersToGroups(freeIpaClient, getGroupUserMapping(users));

        // TODO remove users from groups that are not specified
        // TODO remove groups that are not specified
        // TODO remove/disable users

        LOGGER.info("Done!");
    }

    private void addGroups(FreeIpaClient freeIpaClient, Set<Group> groups) throws FreeIpaClientException {
        for (Group group : groups) {
            LOGGER.debug("adding group {}", group.getName());
            RPCResponse<Object> groupAdd = freeIpaClient.groupAdd(group.getName());
            LOGGER.debug("Success: {}", groupAdd.getResult());
        }
    }

    private void addUsers(FreeIpaClient freeIpaClient, Set<User> users) throws FreeIpaClientException {
        for (User user : users) {
            String username = user.getName();

            LOGGER.debug("adding user {}", username);

            RPCResponse<com.sequenceiq.freeipa.client.model.User> userAdd = freeIpaClient.userAdd(
                    username, user.getFirstName(), user.getLastName(), generateRandomPassword());

            LOGGER.debug("Success: {}", userAdd.getResult());
        }
    }

    private void addUsersToGroups(FreeIpaClient freeIpaClient, Map<String, Set<String>> groupMapping) throws FreeIpaClientException {
        for (Map.Entry<String, Set<String>> entry : groupMapping.entrySet()) {
            LOGGER.debug("adding users {} to group {}", entry.getValue(), entry.getKey());
            // TODO specialize response object
            RPCResponse<Object> groupAddMember = freeIpaClient.groupAddMembers(entry.getKey(), entry.getValue());
            LOGGER.debug("Success: {}", groupAddMember.getResult());
        }
    }

    private Map<String, Set<String>> getGroupUserMapping(Set<User> users) {
        Map<String, Set<String>> mapping = new HashMap<>();

        for (User user : users) {
            String username = user.getName();
            for (String group : user.getGroups()) {
                Set<String> members = mapping.get(group);
                if (members == null) {
                    members = new HashSet<>();
                    mapping.put(group, members);
                }
                members.add(username);
            }
        }
        return mapping;
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString();
    }
}