package com.sequenceiq.cloudbreak.authorization;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.INVITE;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.MANAGE;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.READ;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.WRITE;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.ALL;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.BLUEPRINT;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.CREDENTIAL;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.IMAGECATALOG;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.LDAP;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.MPACK;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.ORG;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.PROXY;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.RDS;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.RECIPE;
import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.STACK;

import java.util.Set;

public enum OrganizationPermissions {

    ORG_INVITE(ORG, INVITE, "Invite users into the organization."),
    ORG_MANAGE(ORG, MANAGE, "Manage users and permissions in an organization."),

    ALL_READ(ALL, READ, "Read all resources in an organization."),
    ALL_WRITE(ALL, WRITE, "Write/delete all resources in an organization."),

    BLUEPRINT_READ(BLUEPRINT, READ, "Read blueprints in an organization."),
    BLUEPRINT_WRITE(BLUEPRINT, WRITE, "Write blueprints in an organization."),

    IMAGECATALOG_READ(IMAGECATALOG, READ, "Read image catalogs in an organization."),
    IMAGECATALOG_WRITE(IMAGECATALOG, WRITE, "Write image catalogs in an organization."),

    CREDENTIAL_READ(CREDENTIAL, READ, "Read credentials in an organization."),
    CREDENTIAL_WRITE(CREDENTIAL, WRITE, "Write credentials in an organization."),

    RECIPE_READ(RECIPE, READ, "Read recipes in an organization."),
    RECIPE_WRITE(RECIPE, WRITE, "Write recipes in an organization."),

    STACK_READ(STACK, READ, "Read stacks in an organization."),
    STACK_WRITE(STACK, WRITE, "Write stacks in an organization."),

    LDAPCONFIG_READ(LDAP, READ, "Read ldap configs in an organization."),
    LDAPCONFIG_WRITE(LDAP, WRITE, "Read ldap configs in an organization."),

    RDSCONFIG_READ(RDS, READ, "Read rds configs in an organization."),
    RDSCONFIG_WRITE(RDS, WRITE, "Read rds configs in an organization."),

    PROXYCONFIG_READ(PROXY, READ, "Read proxy configs in an organization."),
    PROXYCONFIG_WRITE(PROXY, WRITE, "Read proxy configs in an organization."),

    MANAGEMENTPACK_READ(MPACK, READ, "Write management pack configs in an organization."),
    MANAGEMENTPACK_WRITE(MPACK, WRITE, "Write management pack configs in an organization.");

    private final String name;

    private final OrganizationResource resource;

    private final Action action;

    private final String description;

    OrganizationPermissions(OrganizationResource resource, Action action, String description) {
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.name = getName(resource, action);
    }

    public static String getName(OrganizationResource resource, Action action) {
        return resource.name() + ':' + action.name();
    }

    public static boolean hasPermission(Set<String> permissions, OrganizationResource resource, Action action) {
        if (resource == ORG) {
            if (action == INVITE && (permissions.contains(ORG_INVITE.name) || permissions.contains(ORG_MANAGE.name))) {
                return true;
            }
            return action == MANAGE && permissions.contains(ORG_MANAGE.name);
        } else if (permissions.contains(ALL_WRITE.name)) {
            return true;
        } else if (permissions.contains(ALL_READ.name) && action == READ) {
            return true;
        }
        String permissionName = getName(resource, action);
        return permissions.contains(permissionName);
    }

    public String value() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static OrganizationPermissions of(String value) {
        for (OrganizationPermissions permission : OrganizationPermissions.values()) {
            if (value.equals(permission.value())) {
                return permission;
            }
        }
        throw new IllegalArgumentException(String.format("Permissions value '%s' not found.", value));
    }

    public static boolean isValid(String value) {
        for (OrganizationPermissions p : values()) {
            if (value.equals(p.value())) {
                return true;
            }
        }
        return false;
    }

    public enum Action {
        READ,
        WRITE,
        INVITE,
        MANAGE
    }
}
