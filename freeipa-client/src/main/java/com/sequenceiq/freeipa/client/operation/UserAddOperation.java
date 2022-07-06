package com.sequenceiq.freeipa.client.operation;

import static com.sequenceiq.freeipa.client.FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;

public class UserAddOperation extends AbstractFreeipaOperation<User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAddOperation.class);

    private String user;

    private String firstName;

    private String lastName;

    private boolean disabled;

    private Optional<String> title;

    private UserAddOperation(String user, String firstName, String lastName, boolean disabled, Optional<String> title) {
        this.user = user;
        this.firstName = firstName;
        this.lastName = lastName;
        this.disabled = disabled;
        this.title = title;
    }

    public static UserAddOperation create(String user, String firstName, String lastName, boolean disabled, Optional<String> title) {
        return new UserAddOperation(user, firstName, lastName, disabled, title);
    }

    @Override
    public String getOperationName() {
        return "user_add";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(user);
    }

    @Override
    protected Map<String, Object> getParams() {
        ImmutableMap.Builder<String, Object> params = ImmutableMap.builder();
        params.put("givenname", firstName);
        params.put("sn", lastName);
        params.put("loginshell", "/bin/bash");
        params.put("random", true);
        params.put("setattr", List.of("krbPasswordExpiration=" + MAX_PASSWORD_EXPIRATION_DATETIME,
                "nsAccountLock=" + disabled));
        title.ifPresent(value -> params.put("title", value));
        return params.build();
    }

    @Override
    public Optional<User> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        FreeIpaChecks.checkUserNotProtected(user, () -> String.format("User '%s' is protected and cannot be added to FreeIPA", user));
        LOGGER.debug("adding user {}", user);
        User user = invoke(freeIpaClient, User.class);
        LOGGER.debug("added user {}", user);
        return Optional.of(user);
    }
}
