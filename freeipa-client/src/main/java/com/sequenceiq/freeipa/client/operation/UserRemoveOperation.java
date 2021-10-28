package com.sequenceiq.freeipa.client.operation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;

public class UserRemoveOperation extends AbstractFreeipaOperation<User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRemoveOperation.class);

    private String userUid;

    private UserRemoveOperation(String userUid) {
        this.userUid = userUid;
    }

    public static UserRemoveOperation create(String userUid) {
        return new UserRemoveOperation(userUid);
    }

    @Override
    public String getOperationName() {
        return "user_del";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(userUid);
    }

    @Override
    protected Map<String, Object> getParams() {
        return Map.of("skipcheck", "TRUE");
    }

    @Override
    public Optional<User> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        FreeIpaChecks.checkUserNotProtected(userUid, () -> String.format("User '%s' is protected and cannot be deleted from FreeIPA", userUid));
        LOGGER.debug("removing user {}", userUid);
        User removed = invoke(freeIpaClient, User.class);
        LOGGER.debug("removed user {}", userUid);
        return Optional.of(removed);
    }
}
