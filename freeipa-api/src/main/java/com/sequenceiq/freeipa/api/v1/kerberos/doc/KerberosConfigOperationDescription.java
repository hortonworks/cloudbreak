package com.sequenceiq.freeipa.api.v1.kerberos.doc;

public class KerberosConfigOperationDescription {
    public static final String KERBEROS_CONFIG_V4_DESCRIPTION = "Operations on kerberos configs.";
    public static final String DESCRIBE_FOR_ENVIRONMENT = "describe kerberos config for the given environment";
    public static final String CREATE_FOR_ENVIRONMENT = "create kerberos config for the given environment";
    public static final String DELETE_BY_ENVIRONMENT = "delete kerberos config of the given environment";
    public static final String GET_REQUEST = "get create request of a kerberos config";
    public static final String GET_BY_ENV_FOR_CLUSTER = "get (or create if not exists) LDAP config with separate user for cluster";

    private KerberosConfigOperationDescription() {
    }
}
