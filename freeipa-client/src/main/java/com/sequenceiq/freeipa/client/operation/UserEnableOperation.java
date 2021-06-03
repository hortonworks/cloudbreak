package com.sequenceiq.freeipa.client.operation;

import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class UserEnableOperation extends AbstractFreeipaOperation<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserEnableOperation.class);

    private String userUid;

    private UserEnableOperation(String userUid) {
        this.userUid = userUid;
    }

    public static UserEnableOperation create(String userUid) {
        return new UserEnableOperation(userUid);
    }

    @Override
    public String getOperationName() {
        return "user_enable";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(userUid);
    }

    @Override
    public Optional<Object> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        FreeIpaChecks.checkUserNotProtected(userUid, () -> String.format("User '%s' is protected and cannot be enabled in FreeIPA", userUid));
        LOGGER.debug("enabling user {}", userUid);
        Object response = invoke(freeIpaClient, Object.class);
        LOGGER.debug("enabled user {}", userUid);
        return Optional.of(response);
    }
}
