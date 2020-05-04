package com.sequenceiq.authorization.resource;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public enum AuthorizationResourceAction {
    CHANGE_CREDENTIAL("changeCredential", ActionType.RESOURCE_DEPENDENT),
    EDIT_CREDENTIAL("editCredential", ActionType.RESOURCE_DEPENDENT),
    EDIT_ENVIRONMENT("editEnvironment", ActionType.RESOURCE_DEPENDENT),
    START_ENVIRONMENT("startEnvironment", ActionType.RESOURCE_DEPENDENT),
    STOP_ENVIRONMENT("stopEnvironment", ActionType.RESOURCE_DEPENDENT),
    DELETE_CREDENTIAL("deleteCredential", ActionType.RESOURCE_DEPENDENT),
    DESCRIBE_CREDENTIAL("describeCredential", ActionType.RESOURCE_DEPENDENT),
    DELETE_ENVIRONMENT("deleteCdpEnvironment", ActionType.RESOURCE_DEPENDENT),
    DESCRIBE_ENVIRONMENT("describeEnvironment", ActionType.RESOURCE_DEPENDENT),
    ACCESS_ENVIRONMENT("accessEnvironment", ActionType.RESOURCE_DEPENDENT),
    ADMIN_FREEIPA("adminFreeIPA", ActionType.RESOURCE_DEPENDENT),
    CREATE_CREDENTIAL("createCredential", ActionType.RESOURCE_INDEPENDENT),
    CREATE_ENVIRONMENT("createEnvironment", ActionType.RESOURCE_INDEPENDENT),
    GET_KEYTAB("getKeytab", ActionType.RESOURCE_INDEPENDENT),
    CREATE_IMAGE_CATALOG("createImageCatalog", ActionType.RESOURCE_INDEPENDENT),
    EDIT_IMAGE_CATALOG("editImageCatalog", ActionType.RESOURCE_DEPENDENT),
    DESCRIBE_IMAGE_CATALOG("describeImageCatalog", ActionType.RESOURCE_DEPENDENT),
    DELETE_IMAGE_CATALOG("deleteImageCatalog", ActionType.RESOURCE_DEPENDENT),
    // deprecated actions, please do not use them
    READ("read", ActionType.RESOURCE_INDEPENDENT),
    WRITE("write", ActionType.RESOURCE_INDEPENDENT);

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
