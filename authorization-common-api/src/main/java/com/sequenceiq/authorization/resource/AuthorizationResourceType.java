package com.sequenceiq.authorization.resource;

public enum AuthorizationResourceType {
    DATALAKE(true),
    ENVIRONMENT(false),
    CREDENTIAL(false),
    DATAHUB(true),
    IMAGE_CATALOG(false),
    CLUSTER_DEFINITION(false),
    CLUSTER_TEMPLATE(false),
    CUSTOM_CONFIGURATIONS(false),
    DATABASE(false),
    DATABASE_SERVER(false),
    RECIPE(false),
    AUDIT_CREDENTIAL(false),
    PROXY(false),
    CONSUMPTION(false),
    KERBEROS(false),
    FREEIPA(false),
    LDAP(false),
    STRUCTURED_EVENT(false),
    ENCRYPTION_PROFILE(false);

    private final boolean hierarchicalAuthorizationNeeded;

    AuthorizationResourceType(boolean hierarchicalAuthorizationNeeded) {
        this.hierarchicalAuthorizationNeeded = hierarchicalAuthorizationNeeded;
    }

    public boolean isHierarchicalAuthorizationNeeded() {
        return hierarchicalAuthorizationNeeded;
    }
}
