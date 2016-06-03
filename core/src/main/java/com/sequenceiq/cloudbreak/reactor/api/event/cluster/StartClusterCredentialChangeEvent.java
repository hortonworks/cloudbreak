package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartClusterCredentialChangeEvent extends StackEvent {
    private final String user;
    private final String password;

    public StartClusterCredentialChangeEvent(Long stackId, String user, String password) {
        this(null, stackId, user, password);
    }

    public StartClusterCredentialChangeEvent(String selector, Long stackId, String user, String password) {
        super(selector, stackId);
        this.user = user;
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
