package com.sequenceiq.freeipa.flow.freeipa.user.event;

public class SetPasswordRequest extends FreeIpaClientRequest<SetPasswordResult> {

    private final String environment;

    private final String username;

    private final String password;

    public SetPasswordRequest(Long stackId, String environment, String username, String password) {
        super(stackId);
        this.environment = environment;
        this.username = username;
        this.password = password;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "SetPasswordRequest{"
                + "stackId='" + getResourceId() + '\''
                + "environment='" + environment + '\''
                + "username='" + username + '\''
                + '}';
    }
}
