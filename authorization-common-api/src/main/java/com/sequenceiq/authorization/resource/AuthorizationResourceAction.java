package com.sequenceiq.authorization.resource;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public enum AuthorizationResourceAction {
    READ("read", ActionType.RESOURCE_INDEPENDENT),
    WRITE("write", ActionType.RESOURCE_INDEPENDENT),
    RD_READ("rdRead", ActionType.RESOURCE_DEPENDENT),
    RD_WRITE("rdWrite", ActionType.RESOURCE_DEPENDENT),
    ACCESS_ENVIRONMENT("accessEnvironment", ActionType.RESOURCE_DEPENDENT),
    ADMIN_FREEIPA("adminFreeIPA", ActionType.RESOURCE_DEPENDENT),
    CREATE("create", ActionType.RESOURCE_INDEPENDENT),
    GET_KEYTAB("getKeytab", ActionType.RESOURCE_INDEPENDENT);

    private final String action;

    private final ActionType actionType;

    AuthorizationResourceAction(String action, ActionType actionType) {
        this.action = action;
        this.actionType = actionType;
    }

    public String getAction() {
        return action;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public static Optional<AuthorizationResourceAction> getByName(String name) {
        return Arrays.stream(AuthorizationResourceAction.values())
                .filter(resource -> StringUtils.equals(resource.getAction(), name))
                .findAny();
    }

    public enum ActionType {
        RESOURCE_DEPENDENT,
        RESOURCE_INDEPENDENT;
    }
}
