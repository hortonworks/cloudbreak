package com.sequenceiq.freeipa.client.operation;

import static com.sequenceiq.freeipa.client.FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;

public class StageUserAddOperation extends AbstractFreeipaOperation<User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StageUserAddOperation.class);

    private String user;

    private String firstName;

    private String lastName;

    private boolean disabled;

    private StageUserAddOperation(String user, String firstName, String lastName, boolean disabled) {
        this.user = user;
        this.firstName = firstName;
        this.lastName = lastName;
        this.disabled = disabled;
    }

    public static StageUserAddOperation create(String user, String firstName, String lastName, boolean disabled) {
        return new StageUserAddOperation(user, firstName, lastName, disabled);
    }

    @Override
    public String getOperationName() {
        return "stageuser_add";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(user);
    }

    @Override
    protected Map<String, Object> getParams() {
        return Map.of(
                "givenname", firstName,
                "sn", lastName,
                "loginshell", "/bin/bash",
                "random", true,
                "setattr", List.of("krbPasswordExpiration=" + MAX_PASSWORD_EXPIRATION_DATETIME,
                        "nsAccountLock=" + disabled)
        );
    }

    @Override
    public Optional<User> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        FreeIpaChecks.checkUserNotProtected(user, () -> String.format("User '%s' is protected and cannot be added to FreeIPA", this.user));
        LOGGER.debug("adding stageuser {}", this.user);
        User user = invoke(freeIpaClient, User.class);
        LOGGER.debug("added stageuser {}", user);
        return Optional.of(user);
    }
}
