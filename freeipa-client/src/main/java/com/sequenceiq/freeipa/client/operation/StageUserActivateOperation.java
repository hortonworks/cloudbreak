package com.sequenceiq.freeipa.client.operation;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;

public class StageUserActivateOperation extends AbstractFreeipaOperation<User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StageUserActivateOperation.class);

    private String user;

    private StageUserActivateOperation(String user) {
        this.user = user;
    }

    public static StageUserActivateOperation create(String user) {
        return new StageUserActivateOperation(user);
    }

    @Override
    public String getOperationName() {
        return "stageuser_activate";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(user);
    }

    @Override
    public Optional<User> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        FreeIpaChecks.checkUserNotProtected(user, () -> String.format("User '%s' is protected and cannot be added to FreeIPA", this.user));
        LOGGER.debug("activating user {}", this.user);
        User user = invoke(freeIpaClient, User.class);
        LOGGER.debug("activated user {}", user);
        return Optional.of(user);
    }
}
