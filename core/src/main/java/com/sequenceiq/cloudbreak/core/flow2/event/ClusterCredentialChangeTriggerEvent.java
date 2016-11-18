package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCredentialChangeTriggerEvent extends StackEvent {
    private final String user;

    private final String password;

    private final Type type;

    private ClusterCredentialChangeTriggerEvent(String selector, Long stackId, String user, String password, Type type) {
        super(selector, stackId);
        this.user = user;
        this.password = password;
        this.type = type;
    }

    public static ClusterCredentialChangeTriggerEvent replaceUserEvent(String selector, Long stackId, String user, String password) {
        return new ClusterCredentialChangeTriggerEvent(selector, stackId, user, password, Type.REPLACE);
    }

    public static ClusterCredentialChangeTriggerEvent changePasswordEvent(String selector, Long stackId, String password) {
        return new ClusterCredentialChangeTriggerEvent(selector, stackId, null, password, Type.UPDATE);
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
