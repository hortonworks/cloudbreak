package com.sequenceiq.freeipa.api.v1.ldap.doc;

public class LdapConfigModelDescription {
    public static final String LDAP_CONFIG_NOTES = "LDAP server integration enables the user to provide"
            + " a central place to store usernames and passwords for the users of his/her clusters.";
    public static final String LDAP_CONFIG_DESCRIPTION = "Operations on LDAP server configurations";
    public static final String LDAP_CONNECTION_RESULT = "result of Ldap connection test";
    public static final String SERVER_HOST = "public host or IP address of LDAP server";
    public static final String SERVER_PORT = "port of LDAP server (typically: 389 or 636 for LDAPS)";
    public static final String PROTOCOL = "determines the protocol (LDAP or LDAP over SSL)";
    public static final String BIND_DN = "bind distinguished name for connection test and group search (e.g. cn=admin,dc=example,dc=org)";
    public static final String BIND_PASSWORD = "password for the provided bind DN";
    public static final String USER_SEARCH_BASE = "template for user search for authentication (e.g. dc=hadoop,dc=apache,dc=org)";
    public static final String USER_DN_PATTERN = "template for pattern based user search for authentication (e.g. cn={0},dc=hadoop,dc=apache,dc=org)";
    public static final String GROUP_SEARCH_BASE = "template for group search for authorization (e.g. dc=hadoop,dc=apache,dc=org)";
    public static final String USER_NAME_ATTRIBUTE = "attribute name for simplified search filter (e.g. sAMAccountName in case of AD, UID or cn for LDAP).";
    public static final String DOMAIN = "domain in LDAP server (e.g. ad.seq.com).";
    public static final String DIRECTORY_TYPE = "directory type of server LDAP or ACTIVE_DIRECTORY and the default is ACTIVE_DIRECTORY ";
    public static final String USER_OBJECT_CLASS = "User Object Class (defaults to person)";
    public static final String GROUP_OBJECT_CLASS = "Group Object Class (defaults to groupOfNames)";
    public static final String GROUP_ID_ATTRIBUTE = "Group Id Attribute (defaults to cn)";
    public static final String GROUP_MEMBER_ATTRIBUTE = "Group Member Attribute (defaults to member)";
    public static final String ADMIN_GROUP = "LDAP group for administrators";
    public static final String USER_GROUP = "LDAP group for regular users";
    public static final String VALIDATION_REQUEST = "Request that contains the minimal set of fields to test LDAP connectivity";
    public static final String CERTIFICATE = "Self-signed certificate of LDAPS server";

    private LdapConfigModelDescription() {
    }
}
