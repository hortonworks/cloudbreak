package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCredentialChangeTriggerEvent extends StackEvent {
    private final String user;

    private final String password;

    private final Type type;

    public ClusterCredentialChangeTriggerEvent(String selector, Long stackId, String user, String password, Type type) {
        super(selector, stackId);
        this.user = user;
        this.password = password;
        this.type = type;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        REPLACE, UPDATE
    }
}
