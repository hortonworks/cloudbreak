package com.sequenceiq.freeipa.client.operation;

import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UserDisableOperation extends AbstractFreeipaOperation<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDisableOperation.class);

    private String userUid;

    private UserDisableOperation(String userUid) {
        this.userUid = userUid;
    }

    public static UserDisableOperation create(String userUid) {
        return new UserDisableOperation(userUid);
    }

    @Override
    public String getOperationName() {
        return "user_disable";
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
    public Optional<Object> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        FreeIpaChecks.checkUserNotProtected(userUid, () -> String.format("User '%s' is protected and cannot be disabled in FreeIPA", userUid));
        LOGGER.debug("disabling user {}", userUid);
        Object response = invoke(freeIpaClient, Object.class);
        LOGGER.debug("disabled user {}", userUid);
        return Optional.of(response);
    }
}
