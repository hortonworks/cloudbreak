package com.sequenceiq.authorization.info.model;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

public enum RightV4 {
    // account level
    ENV_CREATE(AuthorizationResourceAction.CREATE_ENVIRONMENT),
    LIST_ASSIGNED_ROLES(AuthorizationResourceAction.LIST_ASSIGNED_ROLES),
    CREATE_CLUSTER_TEMPLATE(AuthorizationResourceAction.CREATE_CLUSTER_TEMPLATE),
    CREATE_CLUSTER_DEFINITION(AuthorizationResourceAction.CREATE_CLUSTER_DEFINITION),
    CREATE_CREDENTIAL(AuthorizationResourceAction.CREATE_CREDENTIAL),
    CREATE_RECIPE(AuthorizationResourceAction.CREATE_RECIPE),
    CREATE_IMAGE_CATALOG(AuthorizationResourceAction.CREATE_IMAGE_CATALOG),
    // env level
    ENV_START(AuthorizationResourceAction.START_ENVIRONMENT),
    ENV_STOP(AuthorizationResourceAction.STOP_ENVIRONMENT),
    ENV_DELETE(AuthorizationResourceAction.DELETE_ENVIRONMENT),
    ENV_DESCRIBE(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT),
    CHANGE_CRED(AuthorizationResourceAction.CHANGE_CREDENTIAL),
    DH_CREATE(AuthorizationResourceAction.ENVIRONMENT_CREATE_DATAHUB),
    // dh level
    DH_START(AuthorizationResourceAction.START_DATAHUB),
    DH_STOP(AuthorizationResourceAction.STOP_DATAHUB),
    DH_DELETE(AuthorizationResourceAction.DELETE_DATAHUB),
    DH_REPAIR(AuthorizationResourceAction.REPAIR_DATAHUB),
    DH_RESIZE(AuthorizationResourceAction.SCALE_DATAHUB),
    DH_RETRY(AuthorizationResourceAction.RETRY_DATAHUB_OPERATION),
    DH_DESCRIBE(AuthorizationResourceAction.DESCRIBE_DATAHUB),
    DH_UPGRADE(AuthorizationResourceAction.UPGRADE_DATAHUB),
    // sdx level
    SDX_UPGRADE(AuthorizationResourceAction.UPGRADE_DATALAKE),
    SDX_RECOVER(AuthorizationResourceAction.RECOVER_DATALAKE),
    SDX_REPAIR(AuthorizationResourceAction.REPAIR_DATALAKE),
    SDX_RETRY(AuthorizationResourceAction.RETRY_DATALAKE_OPERATION),
    SDX_DESCRIBE(AuthorizationResourceAction.DESCRIBE_DATALAKE),
    // legacy
    DISTROX_READ(AuthorizationResourceAction.DATAHUB_READ),
    DISTROX_WRITE(AuthorizationResourceAction.DATAHUB_WRITE),
    SDX_READ(AuthorizationResourceAction.DATALAKE_READ),
    SDX_WRITE(AuthorizationResourceAction.DATALAKE_WRITE),
    ENVIRONMENT_READ(AuthorizationResourceAction.ENVIRONMENT_READ),
    ENVIRONMENT_WRITE(AuthorizationResourceAction.ENVIRONMENT_WRITE);

    private static final Map<AuthorizationResourceAction, RightV4> BY_ACTION = Stream.of(RightV4.values())
            .collect(Collectors.toUnmodifiableMap(RightV4::getAction, Function.identity()));

    private AuthorizationResourceAction action;

    RightV4(AuthorizationResourceAction action) {
        this.action = action;
    }

    public AuthorizationResourceAction getAction() {
        return action;
    }

    public static RightV4 getByAction(AuthorizationResourceAction authorizationResourceAction) {
        RightV4 result = BY_ACTION.get(authorizationResourceAction);
        if (result == null) {
            throw new NotFoundException(String.format("Action %s not found in this enum", authorizationResourceAction));
        }
        return result;
    }

    @JsonCreator
    public static RightV4 fromString(String string) {
        try {
            return RightV4.valueOf(string);
        } catch (Exception e) {
            return ENVIRONMENT_READ;
        }
    }
}
