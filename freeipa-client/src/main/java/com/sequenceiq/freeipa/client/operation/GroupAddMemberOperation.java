package com.sequenceiq.freeipa.client.operation;

import java.util.Collection;
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
import com.sequenceiq.cloudbreak.client.RPCResponse;

public class GroupAddMemberOperation extends AbstractFreeipaOperation<Group> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupAddMemberOperation.class);

    private String group;

    private Collection<String> users;

    private BiConsumer<String, String> warnings;

    private GroupAddMemberOperation(String group, Collection<String> users, BiConsumer<String, String> warnings) {
        this.group = group;
        this.users = users;
        this.warnings = warnings;
    }

    public static GroupAddMemberOperation create(String group, Collection<String> users, BiConsumer<String, String> warnings) {
        return new GroupAddMemberOperation(group, users, warnings);
    }

    @Override
    public String getOperationName() {
        return "group_add_member";
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
            LOGGER.debug("adding users [{}] to group [{}]", users, group);
            RPCResponse<Group> rpcResponse = rpcInvoke(freeIpaClient, Group.class);
            List<String> members = Optional.ofNullable(rpcResponse.getResult().getMemberUser()).orElse(List.of());
            if (members.containsAll(users)) {
                LOGGER.debug("Successfully added users {} to {}", users, rpcResponse.getResult());
            } else {
                // TODO specialize RPCResponse completed/failed objects
                LOGGER.error("Failed to add {} to group '{}': {}", users, group, rpcResponse.getFailed());
                warnings.accept(group, String.format("Failed to add users to group: %s", rpcResponse.getFailed()));
            }
            return Optional.of(rpcResponse.getResult());
        } catch (FreeIpaClientException e) {
            LOGGER.error("Failed to add {} to group '{}'", users, group, e);
            warnings.accept(group, String.format("Failed to add users %s to group: %s", users, e.getMessage()));
            freeIpaClient.checkIfClientStillUsable(e);
        }
        return Optional.empty();
    }
}
