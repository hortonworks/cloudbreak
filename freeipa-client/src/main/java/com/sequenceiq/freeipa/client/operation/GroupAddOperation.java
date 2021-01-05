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

public class GroupAddOperation extends AbstractFreeipaOperation<Group> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupAddOperation.class);

    private String group;

    private BiConsumer<String, String> warnings;

    private GroupAddOperation(String group, BiConsumer<String, String> warnings) {
        this.group = group;
        this.warnings = warnings;
    }

    public static GroupAddOperation create(String group, BiConsumer<String, String> warnings) {
        return new GroupAddOperation(group, warnings);
    }

    @Override
    public String getOperationName() {
        return "group_add";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(group);
    }

    @Override
    public Optional<Group> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        FreeIpaChecks.checkGroupNotProtected(group, () -> String.format("Group '%s' is protected and cannot be added to FreeIPA", group));
        try {
            LOGGER.debug("adding group {}", group);
            Group groupAdded = invoke(freeIpaClient, Group.class);
            LOGGER.debug("Success: {}", groupAdded);
            return Optional.of(groupAdded);
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                LOGGER.debug("group '{}' already exists", group);
            } else {
                LOGGER.warn("Failed to add group {}", group, e);
                warnings.accept(group, "Failed to add group:" + e.getMessage());
                freeIpaClient.checkIfClientStillUsable(e);
            }
        }
        return Optional.empty();
    }
}
