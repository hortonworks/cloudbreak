package com.sequenceiq.freeipa.client.operation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.freeipa.client.model.RPCResponse;

public class GroupRemoveMemberOperation extends AbstractFreeipaOperation<Group> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupRemoveMemberOperation.class);

    private String group;

    private Collection<String> users;

    private BiConsumer<String, String> warnings;

    private GroupRemoveMemberOperation(String group, Collection<String> users, BiConsumer<String, String> warnings) {
        this.group = group;
        this.users = users;
        this.warnings = warnings;
    }

    public static GroupRemoveMemberOperation create(String group, Collection<String> users, BiConsumer<String, String> warnings) {
        return new GroupRemoveMemberOperation(group, users, warnings);
    }

    @Override
    public String getOperationName() {
        return "group_remove_member";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(group);
    }

    @Override
    protected Map<String, Object> getParams() {
        return Map.of("user", users);
    }

    @Override
    public Optional<Group> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        FreeIpaChecks.checkGroupNotUnmanaged(group, () -> String.format("Group '%s' is not managed and membership cannot be changed", group));
        try {
            LOGGER.debug("removing users {} from group {}", users, group);
            RPCResponse<Group> rpcResponse = rpcInvoke(freeIpaClient, Group.class);
            List<String> members = Optional.ofNullable(rpcResponse.getResult().getMemberUser()).orElse(List.of());
            if (Collections.disjoint(members, users)) {
                LOGGER.debug("Successfully removed users {} from {}", users, rpcResponse.getResult());
            } else {
                // TODO specialize RPCResponse completed/failed objects
                LOGGER.error("Failed to remove {} from group '{}': {}", users, group, rpcResponse.getFailed());
                warnings.accept(group, String.format("Failed to remove users from group: %s", rpcResponse.getFailed()));
            }
            return Optional.of(rpcResponse.getResult());
        } catch (FreeIpaClientException e) {
            LOGGER.error("Failed to remove {} from group '{}'", users, group, e);
            warnings.accept(group, String.format("Failed to remove users %s from group: %s", users, e.getMessage()));
            freeIpaClient.checkIfClientStillUsable(e);
        }
        return Optional.empty();
    }
}
