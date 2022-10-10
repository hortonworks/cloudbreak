package com.sequenceiq.freeipa.api.v1.ldap.doc;

public class LdapConfigOpDescription {
    public static final String POST_CONNECTION_TEST = "test that the connection could be established of an existing or new LDAP config";
    public static final String GET_REQUEST = "get request";
    public static final String LIST = "list LDAP configs";
    public static final String GET_BY_ENV = "get LDAP config by environment crn";
    public static final String CREATE = "create LDAP config";
    public static final String DELETE_BY_ENV = "delete LDAP config by environment crn";
    public static final String GET_BY_ENV_FOR_CLUSTER = "get (or create if not exists) LDAP config with separate user for cluster";
    public static final String GET_BY_ENV_FOR_USERSYNC = "get LDAP config for user sync. Internal only";

    private LdapConfigOpDescription() {
    }
}
