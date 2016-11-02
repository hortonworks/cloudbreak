package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCredentialChangeTriggerEvent extends StackEvent {
    private final String user;

    private final String password;

    public ClusterCredentialChangeTriggerEvent(String selector, Long stackId, String user, String password) {
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
