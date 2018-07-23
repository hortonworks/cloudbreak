package com.sequenceiq.cloudbreak.validation;

import static com.sequenceiq.cloudbreak.validation.Permissions.Action.INVITE;
import static com.sequenceiq.cloudbreak.validation.Permissions.Action.MANAGE;
import static com.sequenceiq.cloudbreak.validation.Permissions.Action.READ;
import static com.sequenceiq.cloudbreak.validation.Permissions.Action.WRITE;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.ALL;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.BLUEPRINT;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.CREDENTIAL;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.IMAGECATALOG;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.LDAP;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.MPACK;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.ORG;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.PROXY;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.RDS;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.RECIPE;
import static com.sequenceiq.cloudbreak.validation.Permissions.Resource.STACK;

public enum Permissions {
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

    private String name;

    private Resource resource;

    private Action action;

    private String description;

    Permissions(Resource resource, Action action, String description) {
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.name = resource.name() + ":" + action.name();
    }

    public String value() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static Permissions of(String value) {
        for (Permissions permission : Permissions.values()) {
            if (value.equals(permission.value())) {
                return permission;
            }
        }
        throw new IllegalArgumentException(String.format("Permissions value '%s' not found.", value));
    }

    public static boolean isValid(String value) {
        for (Permissions p : values()) {
            if (value.equals(p.value())) {
                return true;
            }
        }
        return false;
    }

    enum Resource {
        ALL,
        ORG,
        BLUEPRINT,
        IMAGECATALOG,
        CREDENTIAL,
        RECIPE,
        STACK,
        LDAP,
        RDS,
        PROXY,
        MPACK
    }

    enum Action {
        READ,
        WRITE,
        INVITE,
        MANAGE
    }
}
