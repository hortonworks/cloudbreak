package com.sequenceiq.freeipa.client.operation;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.model.Group;

public class GroupRemoveOperation extends AbstractFreeipaOperation<Group> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupRemoveOperation.class);

    private String group;

    private BiConsumer<String, String> warnings;

    private GroupRemoveOperation(String group, BiConsumer<String, String> warnings) {
        this.group = group;
        this.warnings = warnings;
    }

    public static GroupRemoveOperation create(String group, BiConsumer<String, String> warnings) {
        return new GroupRemoveOperation(group, warnings);
    }

    @Override
    public String getOperationName() {
        return "group_del";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(group);
    }

    @Override
    public Optional<Group> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        FreeIpaChecks.checkGroupNotProtected(group, () -> String.format("Group '%s' is protected and cannot be deleted from FreeIPA", group));
        try {
            LOGGER.debug("Removing group {}", group);
            Group groupRemoved = invoke(freeIpaClient, Group.class);
            LOGGER.debug("Success: {}", groupRemoved);
            return Optional.of(groupRemoved);
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                LOGGER.debug("group '{}' already does not exists", group);
            } else {
                LOGGER.error("Failed to delete {}", group, e);
                warnings.accept(group, "Failed to remove group: " + e.getMessage());
                freeIpaClient.checkIfClientStillUsable(e);
            }
        }
        return Optional.empty();
    }
}
