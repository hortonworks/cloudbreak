package com.sequenceiq.cloudbreak.authorization;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.INVITE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.MANAGE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.READ;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.WRITE;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.ALL;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.CLUSTER_DEFINITION;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.CREDENTIAL;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.FLEXSUBSCRIPTION;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.IMAGECATALOG;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.LDAP;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.MPACK;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.PROXY;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.RDS;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.RECIPE;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.STACK;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.WORKSPACE;

public enum WorkspacePermissions {

    WORKSPACE_INVITE(WORKSPACE, INVITE, "Invite users into the workspace."),
    WORKSPACE_MANAGE(WORKSPACE, MANAGE, "Manage users and permissions in an workspace."),

    ALL_READ(ALL, READ, "Read all resources in an workspace."),
    ALL_WRITE(ALL, WRITE, "Write/delete all resources in an workspace."),

    CLUSTER_DEFINITION_READ(CLUSTER_DEFINITION, READ, "Read cluster definitions in an workspace."),
    CLUSTER_DEFINITION_WRITE(CLUSTER_DEFINITION, WRITE, "Write cluster definitions in an workspace."),

    IMAGECATALOG_READ(IMAGECATALOG, READ, "Read image catalogs in an workspace."),
    IMAGECATALOG_WRITE(IMAGECATALOG, WRITE, "Write image catalogs in an workspace."),

    CREDENTIAL_READ(CREDENTIAL, READ, "Read credentials in an workspace."),
    CREDENTIAL_WRITE(CREDENTIAL, WRITE, "Write credentials in an workspace."),

    RECIPE_READ(RECIPE, READ, "Read recipes in an workspace."),
    RECIPE_WRITE(RECIPE, WRITE, "Write recipes in an workspace."),

    STACK_READ(STACK, READ, "Read stacks in an workspace."),
    STACK_WRITE(STACK, WRITE, "Write stacks in an workspace."),

    LDAPCONFIG_READ(LDAP, READ, "Read ldap configs in an workspace."),
    LDAPCONFIG_WRITE(LDAP, WRITE, "Read ldap configs in an workspace."),

    RDSCONFIG_READ(RDS, READ, "Read rds configs in an workspace."),
    RDSCONFIG_WRITE(RDS, WRITE, "Read rds configs in an workspace."),

    PROXYCONFIG_READ(PROXY, READ, "Read proxy configs in an workspace."),
    PROXYCONFIG_WRITE(PROXY, WRITE, "Read proxy configs in an workspace."),

    MANAGEMENTPACK_READ(MPACK, READ, "Write management pack configs in an workspace."),
    MANAGEMENTPACK_WRITE(MPACK, WRITE, "Write management pack configs in an workspace."),

    FLEXSUBSCRIPTION_READ(FLEXSUBSCRIPTION, READ, "Write management pack configs in an workspace."),
    FLEXSUBSCRIPTION_WRITE(FLEXSUBSCRIPTION, WRITE, "Write management pack configs in an workspace.");

    private final String name;

    private final WorkspaceResource resource;

    private final Action action;

    private final String description;

    WorkspacePermissions(WorkspaceResource resource, Action action, String description) {
        this.resource = resource;
        this.action = action;
        this.description = description;
        name = getName(resource, action);
    }

    public static String getName(WorkspaceResource resource, Action action) {
        return resource.name() + ':' + action.name();
    }

    public String value() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static WorkspacePermissions of(String value) {
        for (WorkspacePermissions permission : WorkspacePermissions.values()) {
            if (value.equals(permission.value())) {
                return permission;
            }
        }
        throw new IllegalArgumentException(String.format("Permissions value '%s' not found.", value));
    }

    public static boolean isValid(String value) {
        for (WorkspacePermissions p : values()) {
            if (value.equals(p.value())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNotValid(String value) {
        return !isValid(value);
    }

    public enum Action {
        READ,
        WRITE,
        INVITE,
        MANAGE
    }
}
