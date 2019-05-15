package com.sequenceiq.freeipa.api.v1.kerberos.doc;

public class KerberosConfigModelDescription {
    public static final String KERBEROS_ADMIN = "kerberos admin user";
    public static final String KERBEROS_PASSWORD = "kerberos admin password";
    public static final String KERBEROS_URL = "kerberos KDC server URL";
    public static final String KERBEROS_ADMIN_URL = "kerberos admin server URL";
    public static final String KERBEROS_PRINCIPAL = "kerberos principal";
    public static final String KERBEROS_REALM = "kerberos realm";
    public static final String KERBEROS_CONTAINER_DN = "kerberos containerDn";
    public static final String KERBEROS_LDAP_URL = "URL of the connected ldap";
    public static final String KERBEROS_CONFIG_NAME = "the name of the kerberos configuration";
    public static final String KERBEROS_TCP_ALLOW = "kerberos configuration name";
    public static final String DESCRIPTOR = "Ambari kerberos descriptor";
    public static final String KRB_5_CONF = "Ambari kerberos krb5.conf template";
    public static final String KERBEROS_DOMAIN = "cluster instances will set this as the domain part of their hostname";
    public static final String KERBEROS_NAMESERVERS = "comma separated list of nameservers' IP address which will be used by cluster instances";
    public static final String KERBEROS_KDC_VERIFY_KDC_TRUST = "Allows to select either a trusting SSL connection or a "
            + "validating (non-trusting) SSL connection to KDC";

    private KerberosConfigModelDescription() {
    }
}
