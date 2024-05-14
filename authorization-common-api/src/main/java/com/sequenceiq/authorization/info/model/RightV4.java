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
    CREATE_PROXY(AuthorizationResourceAction.CREATE_PROXY),
    // env level
    ENV_START(AuthorizationResourceAction.START_ENVIRONMENT),
    ENV_STOP(AuthorizationResourceAction.STOP_ENVIRONMENT),
    ENV_DELETE(AuthorizationResourceAction.DELETE_ENVIRONMENT),
    ENV_DESCRIBE(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT),
    ENV_EDIT(AuthorizationResourceAction.EDIT_ENVIRONMENT),
    CHANGE_CRED(AuthorizationResourceAction.CHANGE_CREDENTIAL),
    DH_CREATE(AuthorizationResourceAction.ENVIRONMENT_CREATE_DATAHUB),
    UPDATE_AZURE_ENCRYPTION_RESOURCES(AuthorizationResourceAction.UPDATE_AZURE_ENCRYPTION_RESOURCES),
    ENV_VERTICAL_SCALING(AuthorizationResourceAction.ENVIRONMENT_VERTICAL_SCALING),
    FREEIPA_SECRETS_ROTATE(AuthorizationResourceAction.ROTATE_FREEIPA_SECRETS),
    // dh level
    DH_START(AuthorizationResourceAction.START_DATAHUB),
    DH_STOP(AuthorizationResourceAction.STOP_DATAHUB),
    DH_DELETE(AuthorizationResourceAction.DELETE_DATAHUB),
    DH_REPAIR(AuthorizationResourceAction.REPAIR_DATAHUB),
    DH_RESIZE(AuthorizationResourceAction.SCALE_DATAHUB),
    DH_RETRY(AuthorizationResourceAction.RETRY_DATAHUB_OPERATION),
    DH_DESCRIBE(AuthorizationResourceAction.DESCRIBE_DATAHUB),
    DH_RECOVER(AuthorizationResourceAction.RECOVER_DATAHUB),
    DH_UPGRADE(AuthorizationResourceAction.UPGRADE_DATAHUB),
    DH_REFRESH_RECIPES(AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB),
    DH_VERTICAL_SCALING(AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING),
    DH_SECRETS_ROTATE(AuthorizationResourceAction.ROTATE_DH_SECRETS),
    // sdx level
    SDX_UPGRADE(AuthorizationResourceAction.UPGRADE_DATALAKE),
    SDX_RECOVER(AuthorizationResourceAction.RECOVER_DATALAKE),
    SDX_REPAIR(AuthorizationResourceAction.REPAIR_DATALAKE),
    SDX_RETRY(AuthorizationResourceAction.RETRY_DATALAKE_OPERATION),
    SDX_DESCRIBE(AuthorizationResourceAction.DESCRIBE_DATALAKE),
    SDX_RESIZE(AuthorizationResourceAction.RESIZE_DATALAKE),
    SDX_VERTICAL_SCALING(AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING),
    SDX_REFRESH_RECIPES(AuthorizationResourceAction.REFRESH_RECIPES_DATALAKE),
    SDX_SECRETS_ROTATE(AuthorizationResourceAction.ROTATE_DL_SECRETS);

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
            return ENV_DESCRIBE;
        }
    }
}
