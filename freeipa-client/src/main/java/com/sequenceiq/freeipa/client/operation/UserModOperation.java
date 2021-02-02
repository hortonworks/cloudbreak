package com.sequenceiq.freeipa.client.operation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.model.User;

public class UserModOperation extends AbstractFreeipaOperation<User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserModOperation.class);

    private String key;

    private Object value;

    private String user;

    public UserModOperation() {
    }

    private UserModOperation(String key, Object value, String user) {
        this.key = key;
        this.value = value;
        this.user = user;
    }

    public static UserModOperation create(String key, Object value, String user) {
        return new UserModOperation(key, value, user);
    }

    @Override
    public String getOperationName() {
        return "user_mod";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(user);
    }

    @Override
    protected Map<String, Object> getParams() {
        return sensitiveMap(key, value);
    }

    @Override
    public Optional<User> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        try {
            LOGGER.debug("modifying user {}", user);
            User modified = invoke(freeIpaClient, User.class);
            LOGGER.debug("modified user {}", user);
            return Optional.of(modified);
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isEmptyModlistException(e)) {
                LOGGER.debug("Workload credentials for user '{}' already set.", getUser());
            } else {
                throw e;
            }
        }
        return Optional.empty();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
